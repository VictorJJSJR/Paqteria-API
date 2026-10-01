package mx.paqteria.api.autenticacion;

import mx.paqteria.api.comun.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacion")
public class ControladorAutenticacion {
    private final ServicioAutenticacion servicio;

    public ControladorAutenticacion(ServicioAutenticacion servicio) { this.servicio = servicio; }

    @PostMapping("/iniciar-sesion")
    public RespuestaSesion iniciar(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        return servicio.iniciar(solicitud);
    }

    @GetMapping("/yo")
    public UsuarioPublico yo(JwtAuthenticationToken autenticacion) {
        UsuarioActual usuario = UsuarioActual.desde(autenticacion);
        return new UsuarioPublico(usuario.id(), usuario.nombre(), usuario.correo(), usuario.rol());
    }
}
