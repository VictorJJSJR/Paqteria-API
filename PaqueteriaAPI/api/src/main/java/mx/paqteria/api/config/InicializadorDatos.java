package mx.paqteria.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InicializadorDatos implements ApplicationRunner {
    private static final List<String> ROLES = List.of("ADMINISTRADOR", "OPERADOR", "REPARTIDOR", "CLIENTE");
    private final JdbcTemplate jdbc;
    private final PasswordEncoder codificador;
    private final String correoAdmin;
    private final String nombreAdmin;
    private final String contrasenaAdmin;

    public InicializadorDatos(JdbcTemplate jdbc, PasswordEncoder codificador,
            @Value("${paqteria.inicial.admin-correo:}") String correoAdmin,
            @Value("${paqteria.inicial.admin-nombre}") String nombreAdmin,
            @Value("${paqteria.inicial.admin-contrasena:}") String contrasenaAdmin) {
        this.jdbc = jdbc;
        this.codificador = codificador;
        this.correoAdmin = correoAdmin;
        this.nombreAdmin = nombreAdmin;
        this.contrasenaAdmin = contrasenaAdmin;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String rol : ROLES) {
            Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM ROLES WHERE UPPER(nombre) = ?", Integer.class, rol);
            if (existe == null || existe == 0) jdbc.update("INSERT INTO ROLES (nombre) VALUES (?)", rol);
        }
        if (correoAdmin.isBlank() && contrasenaAdmin.isBlank()) return;
        if (correoAdmin.isBlank() || contrasenaAdmin.length() < 12) {
            throw new IllegalStateException("El administrador inicial requiere correo y una contraseña de al menos 12 caracteres.");
        }
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM USUARIOS WHERE LOWER(email) = LOWER(?)", Integer.class, correoAdmin);
        if (existe != null && existe > 0) return;
        Integer rolId = jdbc.queryForObject("SELECT id_rol FROM ROLES WHERE UPPER(nombre) = 'ADMINISTRADOR'", Integer.class);
        jdbc.update("INSERT INTO USUARIOS (id_rol, nombre, email, password_hash) VALUES (?, ?, ?, ?)",
            rolId, nombreAdmin, correoAdmin.trim(), codificador.encode(contrasenaAdmin));
    }
}
