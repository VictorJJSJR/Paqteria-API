package mx.paqteria.api.autenticacion;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import mx.paqteria.api.comun.ExcepcionApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class ServicioAutenticacion {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder codificador;
    private final JwtEncoder jwtEncoder;
    private final String emisor;
    private final long minutosVigencia;

    public ServicioAutenticacion(JdbcTemplate jdbc, PasswordEncoder codificador, JwtEncoder jwtEncoder,
            @Value("${paqteria.jwt.emisor}") String emisor,
            @Value("${paqteria.jwt.minutos-vigencia}") long minutosVigencia) {
        this.jdbc = jdbc;
        this.codificador = codificador;
        this.jwtEncoder = jwtEncoder;
        this.emisor = emisor;
        this.minutosVigencia = minutosVigencia;
    }

    public RespuestaSesion iniciar(SolicitudInicioSesion solicitud) {
        var usuarios = jdbc.query("""
            SELECT u.id_usuario, u.nombre, u.email, u.password_hash, r.nombre AS rol
            FROM USUARIOS u INNER JOIN ROLES r ON r.id_rol = u.id_rol
            WHERE LOWER(u.email) = LOWER(?)
            """, (rs, fila) -> new UsuarioCredencial(rs.getInt("id_usuario"), rs.getString("nombre"),
                rs.getString("email"), rs.getString("password_hash"), rs.getString("rol")), solicitud.correo().trim());
        if (usuarios.isEmpty() || !codificador.matches(solicitud.contrasena(), usuarios.get(0).contrasenaHash())) {
            throw new ExcepcionApi(HttpStatus.UNAUTHORIZED, "El correo o la contraseña no son correctos.");
        }
        UsuarioCredencial usuario = usuarios.get(0);
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(minutosVigencia, ChronoUnit.MINUTES);
        List<String> roles = List.of("ROLE_" + usuario.rol().trim().toUpperCase());
        JwtClaimsSet reclamos = JwtClaimsSet.builder()
            .issuer(emisor).issuedAt(ahora).expiresAt(expira).subject(Integer.toString(usuario.id()))
            .claim("nombre", usuario.nombre()).claim("correo", usuario.correo()).claim("roles", roles).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), reclamos)).getTokenValue();
        return new RespuestaSesion(token, "Bearer", expira, new UsuarioPublico(usuario.id(), usuario.nombre(), usuario.correo(), usuario.rol()));
    }

    private record UsuarioCredencial(int id, String nombre, String correo, String contrasenaHash, String rol) {}
}
