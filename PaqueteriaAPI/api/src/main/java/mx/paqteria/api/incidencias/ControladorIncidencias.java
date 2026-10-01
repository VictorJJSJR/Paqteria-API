package mx.paqteria.api.incidencias;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import mx.paqteria.api.comun.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidencias")
public class ControladorIncidencias {
    private final ServicioIncidencias servicio;
    public ControladorIncidencias(ServicioIncidencias servicio) { this.servicio = servicio; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR','REPARTIDOR')")
    public List<IncidenciaDto> listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            JwtAuthenticationToken autenticacion) {
        return servicio.listar(fecha == null ? LocalDate.now() : fecha, UsuarioActual.desde(autenticacion));
    }

    @PostMapping
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<IncidenciaDto> crear(@Valid @RequestBody SolicitudCrearIncidencia solicitud, JwtAuthenticationToken autenticacion) {
        IncidenciaDto creado = servicio.crear(solicitud, UsuarioActual.desde(autenticacion));
        return ResponseEntity.created(URI.create("/api/incidencias/" + creado.idIncidencia())).body(creado);
    }
}
