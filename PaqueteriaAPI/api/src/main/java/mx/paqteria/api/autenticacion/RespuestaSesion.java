package mx.paqteria.api.autenticacion;

import java.time.Instant;

public record RespuestaSesion(String token, String tipoToken, Instant expiraEn, UsuarioPublico usuario) {}
