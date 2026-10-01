package mx.paqteria.api.reportes;

import java.time.LocalDate;

public record ResumenDto(LocalDate fecha, long paquetesAsignados, long paquetesEnRuta, long entregasExitosas,
    long paradasPendientes, long rutasActivas, long incidenciasHoy) {}
