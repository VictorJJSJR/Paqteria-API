package mx.paqteria.api.entregas;

import java.net.URI;

import jakarta.validation.Valid;
import mx.paqteria.api.comun.UsuarioActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entregas")
public class ControladorEntregas {
    private final ServicioEntregas servicio;
    public ControladorEntregas(ServicioEntregas servicio) { this.servicio = servicio; }

    @PostMapping
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<ComprobanteDto> completar(@Valid @RequestBody SolicitudCompletarEntrega solicitud,
            JwtAuthenticationToken autenticacion) {
        ComprobanteDto creado = servicio.completar(solicitud, UsuarioActual.desde(autenticacion));
        return ResponseEntity.created(URI.create("/api/entregas/" + creado.idComprobante())).body(creado);
    }
}
