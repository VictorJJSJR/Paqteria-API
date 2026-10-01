package mx.paqteria.api.entregas;

import java.sql.Timestamp;

import mx.paqteria.api.comun.ExcepcionApi;
import mx.paqteria.api.comun.UsuarioActual;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioEntregas {
    private final JdbcTemplate jdbc;
    public ServicioEntregas(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public ComprobanteDto completar(SolicitudCompletarEntrega solicitud, UsuarioActual repartidor) {
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM PAQUETES WITH (UPDLOCK,HOLDLOCK) WHERE id_paquete=?", Integer.class, solicitud.idPaquete());
        if (existe == null || existe == 0) throw new ExcepcionApi(HttpStatus.NOT_FOUND, "No se encontró el paquete solicitado.");
        String estadoPaquete = jdbc.queryForObject("SELECT estado_actual FROM PAQUETES WITH (UPDLOCK,HOLDLOCK) WHERE id_paquete=?", String.class, solicitud.idPaquete());
        if (estadoPaquete != null && estadoPaquete.trim().equalsIgnoreCase("CANCELADO"))
            throw new ExcepcionApi(HttpStatus.CONFLICT, "No se puede completar un paquete cancelado.");
        Integer asignado = jdbc.queryForObject("""
            SELECT COUNT(*) FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno
            WHERE pr.id_paquete=? AND t.id_repartidor=? AND t.fecha_turno=? AND UPPER(t.estado_turno) IN ('ASIGNADO','EN_CURSO','EN CURSO')
            """, Integer.class, solicitud.idPaquete(), repartidor.id(), java.sql.Date.valueOf(java.time.LocalDate.now()));
        if (asignado == null || asignado == 0) throw new ExcepcionApi(HttpStatus.FORBIDDEN, "El paquete no está asignado a tu ruta de hoy.");
        Integer comprobantes = jdbc.queryForObject("SELECT COUNT(*) FROM COMPROBANTES_ENTREGA WHERE id_paquete=?", Integer.class, solicitud.idPaquete());
        if (comprobantes != null && comprobantes > 0) throw new ExcepcionApi(HttpStatus.CONFLICT, "Este paquete ya tiene un comprobante de entrega.");

        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            var sentencia = conexion.prepareStatement("INSERT INTO COMPROBANTES_ENTREGA (id_paquete,id_repartidor,nombre_receptor,url_foto_evidencia,url_firma_receptor) VALUES (?,?,?,?,?)", new String[] { "id_comprobante" });
            sentencia.setInt(1, solicitud.idPaquete()); sentencia.setInt(2, repartidor.id()); sentencia.setString(3, solicitud.nombreReceptor().trim());
            sentencia.setString(4, solicitud.urlFotoEvidencia()); sentencia.setString(5, solicitud.urlFirmaReceptor()); return sentencia;
        }, llave);
        jdbc.update("UPDATE PAQUETES SET estado_actual='ENTREGADO' WHERE id_paquete=?", solicitud.idPaquete());
        jdbc.update("""
            UPDATE PARADAS_RUTA SET estado_parada='ENTREGADA'
            WHERE id_parada=(SELECT TOP 1 pr.id_parada FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno
                WHERE pr.id_paquete=? AND t.id_repartidor=? AND t.fecha_turno=? ORDER BY t.id_turno DESC)
            """, solicitud.idPaquete(), repartidor.id(), java.sql.Date.valueOf(java.time.LocalDate.now()));
        jdbc.update("INSERT INTO HISTORIAL_SEGUIMIENTO (id_paquete,titulo,descripcion) VALUES (?,?,?)",
            solicitud.idPaquete(), "Entrega completada", "Recibió: " + solicitud.nombreReceptor().trim());
        Number id = llave.getKey();
        return jdbc.queryForObject("""
            SELECT c.id_comprobante,c.id_paquete,p.folio,c.nombre_receptor,c.url_foto_evidencia,c.url_firma_receptor,c.id_repartidor,c.fecha_hora
            FROM COMPROBANTES_ENTREGA c JOIN PAQUETES p ON p.id_paquete=c.id_paquete WHERE c.id_comprobante=?
            """, (rs, fila) -> {
                Timestamp fecha = rs.getTimestamp("fecha_hora");
                return new ComprobanteDto(rs.getInt("id_comprobante"), rs.getInt("id_paquete"), rs.getString("folio"),
                    rs.getString("nombre_receptor"), rs.getString("url_foto_evidencia"), rs.getString("url_firma_receptor"),
                    rs.getInt("id_repartidor"), fecha == null ? null : fecha.toLocalDateTime());
            }, id.intValue());
    }
}
