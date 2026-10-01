package mx.paqteria.api.salud;

import java.time.Instant;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorSalud {
    private final JdbcTemplate jdbc;
    public ControladorSalud(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/api/salud")
    public Map<String, Object> salud() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("estado", "disponible", "baseDeDatos", "conectada", "fechaHora", Instant.now());
    }
}
