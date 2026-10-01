package mx.paqteria.api.incidencias;

import java.time.LocalDateTime;

public record IncidenciaDto(int idIncidencia, int idPaquete, String folio, String tipoIncidencia,
    String comentario, String urlFotoReporte, int idRepartidor, String repartidor, LocalDateTime fechaHora) {}
