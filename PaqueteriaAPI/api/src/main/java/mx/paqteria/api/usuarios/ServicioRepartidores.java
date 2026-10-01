package mx.paqteria.api.usuarios;

import java.util.List;

import mx.paqteria.api.comun.ExcepcionApi;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioRepartidores {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder codificador;
    public ServicioRepartidores(JdbcTemplate jdbc, PasswordEncoder codificador) { this.jdbc = jdbc; this.codificador = codificador; }

    public List<RepartidorDto> listar() {
        return jdbc.query("""
            SELECT u.id_usuario, u.nombre, u.email, u.telefono,
                (SELECT TOP 1 t.estado_turno FROM TURNOS_REPARTIDOR t WHERE t.id_repartidor=u.id_usuario ORDER BY t.fecha_turno DESC, t.id_turno DESC) AS estado_turno,
                (SELECT COUNT(*) FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno WHERE t.id_repartidor=u.id_usuario AND t.fecha_turno=CONVERT(date, GETDATE())) AS paradas_hoy
            FROM USUARIOS u JOIN ROLES r ON r.id_rol=u.id_rol
            WHERE UPPER(r.nombre)='REPARTIDOR' ORDER BY u.nombre
            """, (rs, fila) -> new RepartidorDto(rs.getInt("id_usuario"), rs.getString("nombre"), rs.getString("email"),
                rs.getString("telefono"), rs.getString("estado_turno"), rs.getLong("paradas_hoy")));
    }

    @Transactional
    public RepartidorDto crear(SolicitudCrearRepartidor solicitud) {
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS WHERE LOWER(email)=LOWER(?)", Integer.class, solicitud.correo().trim());
        if (existe != null && existe > 0) throw new ExcepcionApi(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        Integer idRol = jdbc.queryForObject("SELECT id_rol FROM ROLES WHERE UPPER(nombre)='REPARTIDOR'", Integer.class);
        jdbc.update("INSERT INTO USUARIOS (id_rol, nombre, email, telefono, password_hash) VALUES (?, ?, ?, ?, ?)",
            idRol, solicitud.nombre().trim(), solicitud.correo().trim(), solicitud.telefono(), codificador.encode(solicitud.contrasenaInicial()));
        Integer id = jdbc.queryForObject("SELECT id_usuario FROM USUARIOS WHERE LOWER(email)=LOWER(?)", Integer.class, solicitud.correo().trim());
        return new RepartidorDto(id, solicitud.nombre().trim(), solicitud.correo().trim(), solicitud.telefono(), null, 0);
    }
}
