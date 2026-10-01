package mx.paqteria.api.rutas;

import jakarta.validation.constraints.NotBlank;

public record SolicitudEstadoRuta(@NotBlank String estadoTurno) {}
