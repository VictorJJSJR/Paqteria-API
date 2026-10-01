package mx.paqteria.api.entregas;

import java.time.LocalDateTime;

public record ComprobanteDto(int idComprobante, int idPaquete, String folio, String nombreReceptor,
    String urlFotoEvidencia, String urlFirmaReceptor, int idRepartidor, LocalDateTime fechaHora) {}
