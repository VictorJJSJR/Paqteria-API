package mx.paqteria.api.rutas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SolicitudCrearRuta(@Positive int idRepartidor, @Positive int idUnidad,
    @NotNull LocalDate fechaTurno, @NotBlank @Size(max = 50) String estadoTurno,
    @DecimalMin("0.01") BigDecimal tiempoEstimadoHoras, @NotEmpty List<@NotNull @Positive Integer> paquetes) {}
