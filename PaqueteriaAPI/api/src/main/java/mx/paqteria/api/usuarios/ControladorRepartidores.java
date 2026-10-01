package mx.paqteria.api.usuarios;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repartidores")
public class ControladorRepartidores {
    private final ServicioRepartidores servicio;
    public ControladorRepartidores(ServicioRepartidores servicio) { this.servicio = servicio; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR')")
    public List<RepartidorDto> listar() { return servicio.listar(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<RepartidorDto> crear(@Valid @RequestBody SolicitudCrearRepartidor solicitud) {
        RepartidorDto creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/repartidores/" + creado.idUsuario())).body(creado);
    }
}
