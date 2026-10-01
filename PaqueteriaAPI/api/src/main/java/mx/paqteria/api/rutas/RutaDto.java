package mx.paqteria.api.rutas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RutaDto(int idTurno, int idRepartidor, String repartidor, int idUnidad, String codigoUnidad,
    String placas, String centro, LocalDate fecha, String estado, int totalPaquetes,
    BigDecimal tiempoEstimadoHoras, List<ParadaDto> paradas) {}
