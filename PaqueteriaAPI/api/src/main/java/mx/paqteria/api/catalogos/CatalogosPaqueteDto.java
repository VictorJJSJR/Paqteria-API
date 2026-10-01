package mx.paqteria.api.catalogos;

import java.util.List;

public record CatalogosPaqueteDto(List<CentroDto> centros, List<ClienteDto> clientes) {
    public record CentroDto(int idCentro, String nombre, String ciudad) {}
    public record ClienteDto(int idUsuario, String nombre, String correo) {}
}
