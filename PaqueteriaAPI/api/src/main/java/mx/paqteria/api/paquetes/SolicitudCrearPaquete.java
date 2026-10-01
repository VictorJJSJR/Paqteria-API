package mx.paqteria.api.paquetes;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SolicitudCrearPaquete(
    @NotBlank @Size(max = 50) String folio,
    @NotNull @Positive Integer idCliente,
    @NotNull @Positive Integer idCentroOrigen,
    @NotBlank @Size(max = 255) String direccionOrigen,
    @NotBlank @Size(max = 255) String direccionDestino,
    @Size(max = 100) String coordenadasDestino,
    @NotNull @DecimalMin("0.01") BigDecimal pesoKg,
    @Size(max = 50) String tamanoEtiqueta,
    Boolean prioritario,
    Boolean fragil,
    @NotBlank @Size(max = 50) String estadoActual
) {}
