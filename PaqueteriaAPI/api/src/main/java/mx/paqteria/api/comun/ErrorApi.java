package mx.paqteria.api.comun;

import java.time.Instant;

public record ErrorApi(Instant fechaHora, int estado, String codigo, String mensaje, String ruta) {}
