package mx.paqteria.api.rutas;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import mx.paqteria.api.comun.UsuarioActual;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioRutas {
    private final JdbcTemplate jdbc;
    public ServicioRutas(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<RutaDto> listar(java.time.LocalDate fecha, UsuarioActual usuario) {
        String filtroRepartidor = usuario.rol().equalsIgnoreCase("REPARTIDOR") ? " AND t.id_repartidor=? " : "";
        String consulta = """
            SELECT t.id_turno, t.id_repartidor, u.nombre AS repartidor, t.id_unidad,
                   v.codigo_unidad, v.placas, c.nombre AS centro, t.fecha_turno,
                   t.estado_turno, t.total_paquetes, t.tiempo_estimado_horas
            FROM TURNOS_REPARTIDOR t
            INNER JOIN USUARIOS u ON u.id_usuario=t.id_repartidor
            INNER JOIN UNIDADES v ON v.id_unidad=t.id_unidad
            INNER JOIN CENTROS_DISTRIBUCION c ON c.id_centro=v.id_centro
            WHERE t.fecha_turno=?
            """ + filtroRepartidor + " ORDER BY t.id_turno DESC";
        List<Object> args = new ArrayList<>(); args.add(Date.valueOf(fecha));
        if (!filtroRepartidor.isEmpty()) args.add(usuario.id());
        List<CabeceraRuta> cabeceras = jdbc.query(consulta, (rs, fila) -> new CabeceraRuta(rs.getInt("id_turno"), rs.getInt("id_repartidor"),
            rs.getString("repartidor"), rs.getInt("id_unidad"), rs.getString("codigo_unidad"), rs.getString("placas"), rs.getString("centro"),
            rs.getDate("fecha_turno").toLocalDate(), rs.getString("estado_turno"), rs.getInt("total_paquetes"), rs.getBigDecimal("tiempo_estimado_horas")), args.toArray());
        return cabeceras.stream().map(c -> new RutaDto(c.idTurno(), c.idRepartidor(), c.repartidor(), c.idUnidad(), c.codigoUnidad(), c.placas(),
            c.centro(), c.fecha(), c.estado(), c.totalPaquetes(), c.tiempoEstimadoHoras(), obtenerParadas(c.idTurno()))).toList();
    }

    @Transactional
    public RutaDto crear(SolicitudCrearRuta solicitud, UsuarioActual operador) {
        if (new HashSet<>(solicitud.paquetes()).size() != solicitud.paquetes().size())
            throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.BAD_REQUEST, "La ruta contiene folios de paquete repetidos.");
        Integer esRepartidor = jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS u JOIN ROLES r ON r.id_rol=u.id_rol WHERE u.id_usuario=? AND UPPER(r.nombre)='REPARTIDOR'", Integer.class, solicitud.idRepartidor());
        if (esRepartidor == null || esRepartidor == 0)
            throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.BAD_REQUEST, "El usuario seleccionado no tiene el rol REPARTIDOR.");
        Integer unidadExiste = jdbc.queryForObject("SELECT COUNT(*) FROM UNIDADES WHERE id_unidad=?", Integer.class, solicitud.idUnidad());
        if (unidadExiste == null || unidadExiste == 0)
            throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.BAD_REQUEST, "La unidad seleccionada no existe.");
        Integer turnoRepartidorActivo = jdbc.queryForObject("SELECT COUNT(*) FROM TURNOS_REPARTIDOR WHERE id_repartidor=? AND fecha_turno=? AND UPPER(estado_turno) IN ('ASIGNADO','EN_CURSO','EN CURSO')", Integer.class, solicitud.idRepartidor(), Date.valueOf(solicitud.fechaTurno()));
        if (turnoRepartidorActivo != null && turnoRepartidorActivo > 0)
            throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.CONFLICT, "El repartidor ya tiene un turno asignado para esa fecha.");
        Integer unidadActiva = jdbc.queryForObject("SELECT COUNT(*) FROM TURNOS_REPARTIDOR WHERE id_unidad=? AND fecha_turno=? AND UPPER(estado_turno) IN ('ASIGNADO','EN_CURSO','EN CURSO')", Integer.class, solicitud.idUnidad(), Date.valueOf(solicitud.fechaTurno()));
        if (unidadActiva != null && unidadActiva > 0)
            throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.CONFLICT, "La unidad ya está asignada a un turno para esa fecha.");
        for (Integer idPaquete : solicitud.paquetes()) {
            Integer paqueteExiste = jdbc.queryForObject("SELECT COUNT(*) FROM PAQUETES WHERE id_paquete=?", Integer.class, idPaquete);
            if (paqueteExiste == null || paqueteExiste == 0)
                throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.BAD_REQUEST, "Uno de los paquetes seleccionados no existe.");
            String estadoPaquete = jdbc.queryForObject("SELECT estado_actual FROM PAQUETES WHERE id_paquete=?", String.class, idPaquete);
            if (estadoPaquete != null && Set.of("ENTREGADO", "CANCELADO").contains(estadoPaquete.trim().toUpperCase()))
                throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.CONFLICT, "No se puede asignar un paquete entregado o cancelado.");
            Integer asignado = jdbc.queryForObject("""
                SELECT COUNT(*) FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno
                WHERE pr.id_paquete=? AND t.fecha_turno=? AND UPPER(pr.estado_parada) NOT IN ('ENTREGADA','ENTREGADO','COMPLETADA','COMPLETADO','CANCELADA','CANCELADO')
                """, Integer.class, idPaquete, Date.valueOf(solicitud.fechaTurno()));
            if (asignado != null && asignado > 0)
                throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.CONFLICT, "Un paquete ya está asignado a una ruta en esa fecha.");
        }
        String estado = solicitud.estadoTurno().trim().toUpperCase();
        if (!Set.of("ASIGNADO", "EN_CURSO").contains(estado))
            throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.BAD_REQUEST, "El estado inicial debe ser ASIGNADO o EN_CURSO.");
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            var sentencia = conexion.prepareStatement("INSERT INTO TURNOS_REPARTIDOR (id_repartidor,id_unidad,fecha_turno,estado_turno,total_paquetes,tiempo_estimado_horas) VALUES (?,?,?,?,?,?)", new String[] { "id_turno" });
            sentencia.setInt(1, solicitud.idRepartidor()); sentencia.setInt(2, solicitud.idUnidad()); sentencia.setDate(3, Date.valueOf(solicitud.fechaTurno()));
            sentencia.setString(4, estado); sentencia.setInt(5, solicitud.paquetes().size()); sentencia.setBigDecimal(6, solicitud.tiempoEstimadoHoras()); return sentencia;
        }, llave);
        Number idTurno = llave.getKey();
        if (idTurno == null) throw new IllegalStateException("SQL Server no devolvió el identificador del turno creado.");
        int orden = 1;
        for (Integer idPaquete : solicitud.paquetes()) {
            jdbc.update("INSERT INTO PARADAS_RUTA (id_turno,id_paquete,orden_secuencia,estado_parada) VALUES (?,?,?,'PENDIENTE')", idTurno.intValue(), idPaquete, orden++);
            jdbc.update("UPDATE PAQUETES SET estado_actual='ASIGNADO' WHERE id_paquete=? AND UPPER(estado_actual) NOT IN ('ENTREGADO','CANCELADO')", idPaquete);
            jdbc.update("INSERT INTO HISTORIAL_SEGUIMIENTO (id_paquete,titulo,descripcion) VALUES (?,?,?)", idPaquete, "Paquete asignado a ruta", "Turno " + idTurno.intValue());
        }
        return listar(solicitud.fechaTurno(), operador).stream().filter(r -> r.idTurno() == idTurno.intValue()).findFirst().orElseThrow();
    }

    @Transactional
    public void cambiarEstado(int idTurno, String nuevoEstado, UsuarioActual usuario) {
        String estado = nuevoEstado.trim().toUpperCase();
        if (!Set.of("ASIGNADO", "EN_CURSO", "COMPLETADO", "CANCELADO").contains(estado))
            throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.BAD_REQUEST, "El estado debe ser ASIGNADO, EN_CURSO, COMPLETADO o CANCELADO.");
        String estadoActual;
        try {
            estadoActual = usuario.rol().equalsIgnoreCase("REPARTIDOR")
                ? jdbc.queryForObject("SELECT estado_turno FROM TURNOS_REPARTIDOR WHERE id_turno=? AND id_repartidor=?", String.class, idTurno, usuario.id())
                : jdbc.queryForObject("SELECT estado_turno FROM TURNOS_REPARTIDOR WHERE id_turno=?", String.class, idTurno);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.NOT_FOUND, "No se encontró el turno solicitado.");
        }
        String actual = estadoActual == null ? "" : estadoActual.trim().toUpperCase().replace(' ', '_');
        boolean transicionValida = actual.equals(estado) ||
            (actual.equals("ASIGNADO") && Set.of("EN_CURSO", "CANCELADO").contains(estado)) ||
            (Set.of("EN_CURSO", "EN CURSO").contains(actual) && Set.of("COMPLETADO", "CANCELADO").contains(estado));
        if (!transicionValida) throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.CONFLICT, "El estado solicitado no es una transición válida para este turno.");
        if (estado.equals("COMPLETADO")) {
            Integer pendientes = jdbc.queryForObject("SELECT COUNT(*) FROM PARADAS_RUTA WHERE id_turno=? AND UPPER(estado_parada) NOT IN ('ENTREGADA','ENTREGADO','COMPLETADA','COMPLETADO','CANCELADA','CANCELADO','INCIDENCIA')", Integer.class, idTurno);
            if (pendientes != null && pendientes > 0) throw new mx.paqteria.api.comun.ExcepcionApi(org.springframework.http.HttpStatus.CONFLICT, "No se puede cerrar la ruta mientras queden paradas pendientes.");
        }
        jdbc.update("UPDATE TURNOS_REPARTIDOR SET estado_turno=? WHERE id_turno=?", estado, idTurno);
        if (estado.equals("CANCELADO")) {
            jdbc.update("UPDATE PARADAS_RUTA SET estado_parada='CANCELADA' WHERE id_turno=? AND UPPER(estado_parada) NOT IN ('ENTREGADA','ENTREGADO','COMPLETADA','COMPLETADO','CANCELADA','CANCELADO')", idTurno);
            jdbc.update("""
                UPDATE p SET estado_actual='PENDIENTE'
                FROM PAQUETES p JOIN PARADAS_RUTA pr ON pr.id_paquete=p.id_paquete
                WHERE pr.id_turno=? AND UPPER(p.estado_actual)='ASIGNADO'
                """, idTurno);
        }
    }

    private List<ParadaDto> obtenerParadas(int idTurno) {
        return jdbc.query("""
            SELECT pr.id_parada, pr.orden_secuencia, pr.id_paquete, p.folio, p.direccion_destino,
                   pr.estado_parada, p.es_prioritario, p.es_fragil
            FROM PARADAS_RUTA pr INNER JOIN PAQUETES p ON p.id_paquete=pr.id_paquete
            WHERE pr.id_turno=? ORDER BY pr.orden_secuencia, pr.id_parada
            """, (rs, fila) -> new ParadaDto(rs.getInt("id_parada"), rs.getInt("orden_secuencia"), rs.getInt("id_paquete"),
                rs.getString("folio"), rs.getString("direccion_destino"), rs.getString("estado_parada"), rs.getBoolean("es_prioritario"), rs.getBoolean("es_fragil")), idTurno);
    }

    private record CabeceraRuta(int idTurno, int idRepartidor, String repartidor, int idUnidad, String codigoUnidad,
        String placas, String centro, java.time.LocalDate fecha, String estado, int totalPaquetes, java.math.BigDecimal tiempoEstimadoHoras) {}
}
