package mx.paqteria.api.incidencias;

import java.sql.Timestamp;
import java.time.LocalDate;
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
public class ServicioIncidencias {
    private static final String SELECT = """
        SELECT i.id_incidencia, i.id_paquete, p.folio, i.tipo_incidencia, i.comentario, i.url_foto_reporte,
               i.id_repartidor, u.nombre AS repartidor, i.fecha_hora
        FROM INCIDENCIAS_ENTREGA i JOIN PAQUETES p ON p.id_paquete=i.id_paquete
        JOIN USUARIOS u ON u.id_usuario=i.id_repartidor
        """;
    private final JdbcTemplate jdbc;
    public ServicioIncidencias(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<IncidenciaDto> listar(LocalDate fecha, UsuarioActual usuario) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE CAST(i.fecha_hora AS date)=? ");
        List<Object> args = new ArrayList<>(); args.add(java.sql.Date.valueOf(fecha));
        if (usuario.rol().equalsIgnoreCase("REPARTIDOR")) { sql.append(" AND i.id_repartidor=? "); args.add(usuario.id()); }
        sql.append(" ORDER BY i.fecha_hora DESC, i.id_incidencia DESC");
        return jdbc.query(sql.toString(), (rs, fila) -> mapear(rs), args.toArray());
    }

    @Transactional
    public IncidenciaDto crear(SolicitudCrearIncidencia solicitud, UsuarioActual usuario) {
        Integer asignado = jdbc.queryForObject("""
            SELECT COUNT(*) FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno
            WHERE pr.id_paquete=? AND t.id_repartidor=? AND t.fecha_turno=? AND UPPER(t.estado_turno) IN ('ASIGNADO','EN_CURSO','EN CURSO')
            """, Integer.class, solicitud.idPaquete(), usuario.id(), java.sql.Date.valueOf(LocalDate.now()));
        if (asignado == null || asignado == 0) throw new ExcepcionApi(HttpStatus.FORBIDDEN, "El paquete no está asignado a tu ruta de hoy.");
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            var sentencia = conexion.prepareStatement("INSERT INTO INCIDENCIAS_ENTREGA (id_paquete,id_repartidor,tipo_incidencia,comentario,url_foto_reporte) VALUES (?,?,?,?,?)", new String[] { "id_incidencia" });
            sentencia.setInt(1, solicitud.idPaquete()); sentencia.setInt(2, usuario.id()); sentencia.setString(3, solicitud.tipoIncidencia().trim());
            sentencia.setString(4, solicitud.comentario()); sentencia.setString(5, solicitud.urlFotoReporte()); return sentencia;
        }, llave);
        jdbc.update("INSERT INTO HISTORIAL_SEGUIMIENTO (id_paquete,titulo,descripcion) VALUES (?,?,?)",
            solicitud.idPaquete(), "Incidencia: " + solicitud.tipoIncidencia().trim(), solicitud.comentario());
        jdbc.update("UPDATE PAQUETES SET estado_actual='INCIDENCIA' WHERE id_paquete=?", solicitud.idPaquete());
        jdbc.update("""
            UPDATE PARADAS_RUTA SET estado_parada='INCIDENCIA'
            WHERE id_parada=(SELECT TOP 1 pr.id_parada FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno
                WHERE pr.id_paquete=? AND t.id_repartidor=? AND t.fecha_turno=? ORDER BY t.id_turno DESC)
            """, solicitud.idPaquete(), usuario.id(), java.sql.Date.valueOf(LocalDate.now()));
        Number id = llave.getKey();
        return jdbc.queryForObject(SELECT + " WHERE i.id_incidencia=?", (rs, fila) -> mapear(rs), id.intValue());
    }

    private IncidenciaDto mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp hora = rs.getTimestamp("fecha_hora");
        return new IncidenciaDto(rs.getInt("id_incidencia"), rs.getInt("id_paquete"), rs.getString("folio"),
            rs.getString("tipo_incidencia"), rs.getString("comentario"), rs.getString("url_foto_reporte"),
            rs.getInt("id_repartidor"), rs.getString("repartidor"), hora == null ? null : hora.toLocalDateTime());
    }
}
