package mx.paqteria.api.incidencias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SolicitudCrearIncidencia(@Positive int idPaquete,
    @NotBlank @Size(max = 100) String tipoIncidencia, @Size(max = 4000) String comentario,
    @Size(max = 500) String urlFotoReporte) {}
