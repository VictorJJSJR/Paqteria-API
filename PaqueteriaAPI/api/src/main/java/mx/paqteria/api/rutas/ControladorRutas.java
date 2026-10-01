package mx.paqteria.api.rutas;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import mx.paqteria.api.comun.UsuarioActual;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rutas")
public class ControladorRutas {
    private final ServicioRutas servicio;
    public ControladorRutas(ServicioRutas servicio) { this.servicio = servicio; }

    @GetMapping("/actuales")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR','REPARTIDOR')")
    public List<RutaDto> actuales(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            JwtAuthenticationToken autenticacion) {
        return servicio.listar(fecha == null ? LocalDate.now() : fecha, UsuarioActual.desde(autenticacion));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR')")
    public ResponseEntity<RutaDto> crear(@Valid @RequestBody SolicitudCrearRuta solicitud, JwtAuthenticationToken autenticacion) {
        RutaDto ruta = servicio.crear(solicitud, UsuarioActual.desde(autenticacion));
        return ResponseEntity.status(201).body(ruta);
    }

    @PatchMapping("/{idTurno}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR','REPARTIDOR')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable int idTurno, @Valid @RequestBody SolicitudEstadoRuta solicitud,
            JwtAuthenticationToken autenticacion) {
        servicio.cambiarEstado(idTurno, solicitud.estadoTurno(), UsuarioActual.desde(autenticacion));
        return ResponseEntity.noContent().build();
    }
}
