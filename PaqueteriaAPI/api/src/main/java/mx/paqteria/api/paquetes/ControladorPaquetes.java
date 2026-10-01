package mx.paqteria.api.paquetes;

import java.net.URI;
import java.util.List;

import mx.paqteria.api.comun.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paquetes")
public class ControladorPaquetes {
    private final ServicioPaquetes servicio;
    public ControladorPaquetes(ServicioPaquetes servicio) { this.servicio = servicio; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR','REPARTIDOR','CLIENTE')")
    public List<PaqueteDto> listar(@RequestParam(required = false) String buscar, JwtAuthenticationToken autenticacion) {
        return servicio.listar(buscar, UsuarioActual.desde(autenticacion));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR','REPARTIDOR','CLIENTE')")
    public DetallePaqueteDto detalle(@PathVariable int id, JwtAuthenticationToken autenticacion) {
        return servicio.obtener(id, UsuarioActual.desde(autenticacion));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR')")
    public ResponseEntity<PaqueteDto> crear(@Valid @RequestBody SolicitudCrearPaquete solicitud) {
        PaqueteDto creado = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/paquetes/" + creado.idPaquete())).body(creado);
    }
}
