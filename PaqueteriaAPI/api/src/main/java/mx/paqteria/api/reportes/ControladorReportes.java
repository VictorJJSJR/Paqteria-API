package mx.paqteria.api.reportes;

import java.time.LocalDate;

import mx.paqteria.api.comun.UsuarioActual;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ControladorReportes {
    private final ServicioReportes servicio;
    public ControladorReportes(ServicioReportes servicio) { this.servicio = servicio; }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR','REPARTIDOR')")
    public ResumenDto resumen(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            JwtAuthenticationToken autenticacion) {
        return servicio.resumen(fecha == null ? LocalDate.now() : fecha, UsuarioActual.desde(autenticacion));
    }

    @GetMapping("/reportes/diario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR')")
    public ReporteDiarioDto diario(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.diario(fecha == null ? LocalDate.now() : fecha);
    }
}
