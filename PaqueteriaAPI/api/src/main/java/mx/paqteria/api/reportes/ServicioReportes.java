package mx.paqteria.api.reportes;

import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import mx.paqteria.api.comun.UsuarioActual;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ServicioReportes {
    private final JdbcTemplate jdbc;
    public ServicioReportes(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public ResumenDto resumen(java.time.LocalDate fecha, UsuarioActual usuario) {
        boolean repartidor = usuario.rol().equalsIgnoreCase("REPARTIDOR");
        String extraTurno = repartidor ? " AND id_repartidor=? " : "";
        String extraIncidencia = repartidor ? " AND id_repartidor=? " : "";
        long asignados = contar("SELECT COUNT(*) FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno WHERE t.fecha_turno=?" + extraTurno,
            fecha, repartidor ? usuario.id() : null);
        long pendientes = contar("SELECT COUNT(*) FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno WHERE t.fecha_turno=? AND UPPER(pr.estado_parada) NOT IN ('ENTREGADA','ENTREGADO','COMPLETADA','COMPLETADO','CANCELADA','CANCELADO')" + extraTurno,
            fecha, repartidor ? usuario.id() : null);
        long enRuta = contar("SELECT COUNT(*) FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno WHERE t.fecha_turno=? AND UPPER(pr.estado_parada) IN ('EN_RUTA','EN RUTA','EN CURSO')" + extraTurno,
            fecha, repartidor ? usuario.id() : null);
        long entregas = contar("SELECT COUNT(*) FROM COMPROBANTES_ENTREGA WHERE CAST(fecha_hora AS date)=?" + (repartidor ? " AND id_repartidor=?" : ""),
            fecha, repartidor ? usuario.id() : null);
        long rutas = contar("SELECT COUNT(*) FROM TURNOS_REPARTIDOR WHERE fecha_turno=? AND UPPER(estado_turno) IN ('ASIGNADO','ACTIVO','EN CURSO','EN_CURSO','EN RUTA','EN_RUTA')" + extraTurno,
            fecha, repartidor ? usuario.id() : null);
        long incidencias = contar("SELECT COUNT(*) FROM INCIDENCIAS_ENTREGA WHERE CAST(fecha_hora AS date)=?" + extraIncidencia,
            fecha, repartidor ? usuario.id() : null);
        return new ResumenDto(fecha, asignados, enRuta, entregas, pendientes, rutas, incidencias);
    }

    public ReporteDiarioDto diario(java.time.LocalDate fecha) {
        long creados = contar("SELECT COUNT(*) FROM PAQUETES WHERE CAST(fecha_creacion AS date)=?", fecha, null);
        long entregas = contar("SELECT COUNT(*) FROM COMPROBANTES_ENTREGA WHERE CAST(fecha_hora AS date)=?", fecha, null);
        long rutas = contar("SELECT COUNT(*) FROM TURNOS_REPARTIDOR WHERE fecha_turno=? AND UPPER(estado_turno) IN ('COMPLETADO','COMPLETADA','CERRADO','CERRADA')", fecha, null);
        long incidencias = contar("SELECT COUNT(*) FROM INCIDENCIAS_ENTREGA WHERE CAST(fecha_hora AS date)=?", fecha, null);
        Map<String, Long> porEstado = new LinkedHashMap<>();
        jdbc.query("SELECT estado_actual, COUNT(*) AS total FROM PAQUETES WHERE CAST(fecha_creacion AS date)=? GROUP BY estado_actual ORDER BY estado_actual",
            rs -> porEstado.put(rs.getString("estado_actual"), rs.getLong("total")), Date.valueOf(fecha));
        double efectividad = creados == 0 ? 0.0 : Math.round((entregas * 10000.0) / creados) / 100.0;
        return new ReporteDiarioDto(fecha, creados, entregas, rutas, incidencias, efectividad, porEstado);
    }

    private long contar(String sql, java.time.LocalDate fecha, Integer idRepartidor) {
        Long total = idRepartidor == null
            ? jdbc.queryForObject(sql, Long.class, Date.valueOf(fecha))
            : jdbc.queryForObject(sql, Long.class, Date.valueOf(fecha), idRepartidor);
        return total == null ? 0 : total;
    }
}
