package mx.paqteria.api.paquetes;

import java.time.LocalDateTime;

public record EventoSeguimientoDto(int idHistorial, String titulo, String descripcion, LocalDateTime fechaHora) {}
