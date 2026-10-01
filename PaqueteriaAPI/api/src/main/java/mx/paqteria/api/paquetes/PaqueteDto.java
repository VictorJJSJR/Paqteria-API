package mx.paqteria.api.paquetes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaqueteDto(int idPaquete, String folio, String cliente, String ciudadOrigen,
    String direccionOrigen, String direccionDestino, String coordenadasDestino, BigDecimal pesoKg,
    String tamanoEtiqueta, boolean prioritario, boolean fragil, String estadoActual, LocalDateTime fechaCreacion) {}
