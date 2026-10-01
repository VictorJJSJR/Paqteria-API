package mx.paqteria.api.catalogos;

import java.util.List;

public record CatalogosRutaDto(List<RepartidorDto> repartidores, List<UnidadDto> unidades, List<PaqueteDisponibleDto> paquetes) {
    public record RepartidorDto(int idRepartidor, String nombre) {}
    public record UnidadDto(int idUnidad, String codigoUnidad, String placas, String centro) {}
    public record PaqueteDisponibleDto(int idPaquete, String folio, String direccionDestino, boolean prioritario, boolean fragil, String estadoActual) {}
}
