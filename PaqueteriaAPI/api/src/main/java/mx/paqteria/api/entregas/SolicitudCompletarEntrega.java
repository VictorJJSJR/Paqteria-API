package mx.paqteria.api.entregas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SolicitudCompletarEntrega(@Positive int idPaquete,
    @NotBlank @Size(max = 150) String nombreReceptor,
    @Size(max = 500) String urlFotoEvidencia,
    @Size(max = 500) String urlFirmaReceptor) {}
