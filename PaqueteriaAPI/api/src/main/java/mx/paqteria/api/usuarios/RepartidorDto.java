package mx.paqteria.api.usuarios;

public record RepartidorDto(int idUsuario, String nombre, String correo, String telefono,
    String estadoTurno, long paradasHoy) {}
