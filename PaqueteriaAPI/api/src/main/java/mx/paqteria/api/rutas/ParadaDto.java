package mx.paqteria.api.rutas;

public record ParadaDto(int idParada, int orden, int idPaquete, String folio, String direccionDestino,
    String estado, boolean prioritario, boolean fragil) {}
