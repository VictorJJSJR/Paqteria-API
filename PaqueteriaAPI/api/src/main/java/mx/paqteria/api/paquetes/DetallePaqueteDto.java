package mx.paqteria.api.paquetes;

import java.util.List;

public record DetallePaqueteDto(PaqueteDto paquete, List<EventoSeguimientoDto> seguimiento) {}
