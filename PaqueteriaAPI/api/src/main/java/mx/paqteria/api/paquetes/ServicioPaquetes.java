package mx.paqteria.api.paquetes;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import mx.paqteria.api.comun.ExcepcionApi;
import mx.paqteria.api.comun.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioPaquetes {
    private static final String SELECT_PAQUETE = """
        SELECT p.id_paquete, p.folio, u.nombre AS cliente, c.ciudad AS ciudad_origen,
               p.direccion_origen, p.direccion_destino, p.coordenadas_destino, p.peso_kg,
               p.tamano_etiqueta, p.es_prioritario, p.es_fragil, p.estado_actual, p.fecha_creacion
        FROM PAQUETES p
        INNER JOIN USUARIOS u ON u.id_usuario = p.id_cliente
        INNER JOIN CENTROS_DISTRIBUCION c ON c.id_centro = p.id_centro_origen
        """;
    private final JdbcTemplate jdbc;
    public ServicioPaquetes(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<PaqueteDto> listar(String buscar, UsuarioActual usuario) {
        StringBuilder sql = new StringBuilder(SELECT_PAQUETE).append(" WHERE 1=1 ");
        List<Object> parametros = new ArrayList<>();
        if (buscar != null && !buscar.isBlank()) {
            sql.append(" AND (p.folio LIKE ? OR u.nombre LIKE ? OR p.direccion_destino LIKE ? OR c.ciudad LIKE ?) ");
            String patron = "%" + buscar.trim() + "%";
            parametros.add(patron); parametros.add(patron); parametros.add(patron); parametros.add(patron);
        }
        if (usuario.rol().equalsIgnoreCase("CLIENTE")) {
            sql.append(" AND p.id_cliente = ? "); parametros.add(usuario.id());
        } else if (usuario.rol().equalsIgnoreCase("REPARTIDOR")) {
            sql.append(" AND EXISTS (SELECT 1 FROM PARADAS_RUTA pr INNER JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno WHERE pr.id_paquete=p.id_paquete AND t.id_repartidor=? AND t.fecha_turno=CONVERT(date,GETDATE()) AND UPPER(t.estado_turno) IN ('ASIGNADO','EN_CURSO','EN CURSO')) ");
            parametros.add(usuario.id());
        }
        sql.append(" ORDER BY p.fecha_creacion DESC, p.id_paquete DESC");
        return jdbc.query(sql.toString(), (rs, fila) -> mapearPaquete(rs), parametros.toArray());
    }

    public DetallePaqueteDto obtener(int id, UsuarioActual usuario) {
        List<PaqueteDto> encontrados = jdbc.query(SELECT_PAQUETE + " WHERE p.id_paquete = ?", (rs, fila) -> mapearPaquete(rs), id);
        if (encontrados.isEmpty()) throw new ExcepcionApi(HttpStatus.NOT_FOUND, "No se encontró el paquete solicitado.");
        PaqueteDto paquete = encontrados.get(0);
        if (usuario.rol().equalsIgnoreCase("CLIENTE")) {
            Integer propio = jdbc.queryForObject("SELECT COUNT(*) FROM PAQUETES WHERE id_paquete=? AND id_cliente=?", Integer.class, id, usuario.id());
            if (propio == null || propio == 0) throw new ExcepcionApi(HttpStatus.NOT_FOUND, "No se encontró el paquete solicitado.");
        } else if (usuario.rol().equalsIgnoreCase("REPARTIDOR")) {
            Integer asignado = jdbc.queryForObject("SELECT COUNT(*) FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno WHERE pr.id_paquete=? AND t.id_repartidor=? AND t.fecha_turno=CONVERT(date,GETDATE()) AND UPPER(t.estado_turno) IN ('ASIGNADO','EN_CURSO','EN CURSO')", Integer.class, id, usuario.id());
            if (asignado == null || asignado == 0) throw new ExcepcionApi(HttpStatus.NOT_FOUND, "No se encontró el paquete solicitado.");
        }
        List<EventoSeguimientoDto> eventos = jdbc.query("SELECT id_historial, titulo, descripcion, fecha_hora FROM HISTORIAL_SEGUIMIENTO WHERE id_paquete=? ORDER BY fecha_hora DESC, id_historial DESC",
            (rs, fila) -> new EventoSeguimientoDto(rs.getInt("id_historial"), rs.getString("titulo"), rs.getString("descripcion"), fechaHora(rs.getTimestamp("fecha_hora"))), id);
        return new DetallePaqueteDto(paquete, eventos);
    }

    @Transactional
    public PaqueteDto crear(SolicitudCrearPaquete solicitud) {
        if (!"PENDIENTE".equalsIgnoreCase(solicitud.estadoActual().trim()))
            throw new ExcepcionApi(HttpStatus.BAD_REQUEST, "Un paquete nuevo debe iniciar con estado PENDIENTE.");
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            var sentencia = conexion.prepareStatement("""
                INSERT INTO PAQUETES (folio, id_cliente, id_centro_origen, direccion_origen, direccion_destino,
                    coordenadas_destino, peso_kg, tamano_etiqueta, es_prioritario, es_fragil, estado_actual)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, new String[] { "id_paquete" });
            sentencia.setString(1, solicitud.folio().trim()); sentencia.setInt(2, solicitud.idCliente());
            sentencia.setInt(3, solicitud.idCentroOrigen()); sentencia.setString(4, solicitud.direccionOrigen().trim());
            sentencia.setString(5, solicitud.direccionDestino().trim()); sentencia.setString(6, solicitud.coordenadasDestino());
            sentencia.setBigDecimal(7, solicitud.pesoKg()); sentencia.setString(8, solicitud.tamanoEtiqueta());
            sentencia.setBoolean(9, Boolean.TRUE.equals(solicitud.prioritario())); sentencia.setBoolean(10, Boolean.TRUE.equals(solicitud.fragil()));
            sentencia.setString(11, solicitud.estadoActual().trim()); return sentencia;
        }, llave);
        Number id = llave.getKey();
        if (id == null) throw new IllegalStateException("SQL Server no devolvió el identificador del paquete creado.");
        jdbc.update("INSERT INTO HISTORIAL_SEGUIMIENTO (id_paquete,titulo,descripcion) VALUES (?,?,?)", id.intValue(), "Paquete registrado", "Registro creado desde la operación.");
        return jdbc.queryForObject(SELECT_PAQUETE + " WHERE p.id_paquete=?", (rs, fila) -> mapearPaquete(rs), id.intValue());
    }

    private PaqueteDto mapearPaquete(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PaqueteDto(rs.getInt("id_paquete"), rs.getString("folio"), rs.getString("cliente"), rs.getString("ciudad_origen"),
            rs.getString("direccion_origen"), rs.getString("direccion_destino"), rs.getString("coordenadas_destino"), rs.getBigDecimal("peso_kg"),
            rs.getString("tamano_etiqueta"), rs.getBoolean("es_prioritario"), rs.getBoolean("es_fragil"), rs.getString("estado_actual"), fechaHora(rs.getTimestamp("fecha_creacion")));
    }

    private static java.time.LocalDateTime fechaHora(Timestamp marca) { return marca == null ? null : marca.toLocalDateTime(); }
}
