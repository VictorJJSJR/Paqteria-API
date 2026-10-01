package mx.paqteria.api.reportes;

import java.time.LocalDate;
import java.util.Map;

public record ReporteDiarioDto(LocalDate fecha, long paquetesCreados, long entregasExitosas,
    long rutasCompletadas, long incidenciasRegistradas, double efectividadPorcentaje,
    Map<String, Long> paquetesPorEstado) {}
