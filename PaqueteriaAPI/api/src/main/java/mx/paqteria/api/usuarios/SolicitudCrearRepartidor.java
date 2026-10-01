package mx.paqteria.api.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCrearRepartidor(@NotBlank @Size(max = 150) String nombre,
    @NotBlank @Email @Size(max = 150) String correo, @Size(max = 20) String telefono,
    @NotBlank @Size(min = 12, max = 128) String contrasenaInicial) {}
