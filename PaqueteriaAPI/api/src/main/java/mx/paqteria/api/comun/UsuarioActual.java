package mx.paqteria.api.comun;

import java.util.List;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record UsuarioActual(int id, String nombre, String correo, String rol) {
    public static UsuarioActual desde(JwtAuthenticationToken autenticacion) {
        var jwt = autenticacion.getToken();
        List<String> roles = jwt.getClaimAsStringList("roles");
        String rol = roles == null || roles.isEmpty() ? "" : roles.get(0).replace("ROLE_", "");
        return new UsuarioActual(Integer.parseInt(jwt.getSubject()), jwt.getClaimAsString("nombre"), jwt.getClaimAsString("correo"), rol);
    }
}
