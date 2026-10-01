package mx.paqteria.api.catalogos;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogos")
public class ControladorCatalogos {
    private final JdbcTemplate jdbc;
    public ControladorCatalogos(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/paquetes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR')")
    public CatalogosPaqueteDto paquetes() {
        List<CatalogosPaqueteDto.CentroDto> centros = jdbc.query("SELECT id_centro,nombre,ciudad FROM CENTROS_DISTRIBUCION ORDER BY ciudad,nombre",
            (rs, fila) -> new CatalogosPaqueteDto.CentroDto(rs.getInt("id_centro"), rs.getString("nombre"), rs.getString("ciudad")));
        List<CatalogosPaqueteDto.ClienteDto> clientes = jdbc.query("SELECT u.id_usuario,u.nombre,u.email FROM USUARIOS u JOIN ROLES r ON r.id_rol=u.id_rol WHERE UPPER(r.nombre)='CLIENTE' ORDER BY u.nombre",
            (rs, fila) -> new CatalogosPaqueteDto.ClienteDto(rs.getInt("id_usuario"), rs.getString("nombre"), rs.getString("email")));
        return new CatalogosPaqueteDto(centros, clientes);
    }

    @GetMapping("/rutas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','OPERADOR')")
    public CatalogosRutaDto rutas() {
        List<CatalogosRutaDto.RepartidorDto> repartidores = jdbc.query("SELECT u.id_usuario,u.nombre FROM USUARIOS u JOIN ROLES r ON r.id_rol=u.id_rol WHERE UPPER(r.nombre)='REPARTIDOR' ORDER BY u.nombre",
            (rs, fila) -> new CatalogosRutaDto.RepartidorDto(rs.getInt("id_usuario"), rs.getString("nombre")));
        List<CatalogosRutaDto.UnidadDto> unidades = jdbc.query("SELECT v.id_unidad,v.codigo_unidad,v.placas,c.nombre AS centro FROM UNIDADES v JOIN CENTROS_DISTRIBUCION c ON c.id_centro=v.id_centro ORDER BY v.codigo_unidad",
            (rs, fila) -> new CatalogosRutaDto.UnidadDto(rs.getInt("id_unidad"), rs.getString("codigo_unidad"), rs.getString("placas"), rs.getString("centro")));
        List<CatalogosRutaDto.PaqueteDisponibleDto> paquetes = jdbc.query("""
            SELECT p.id_paquete,p.folio,p.direccion_destino,p.es_prioritario,p.es_fragil,p.estado_actual
            FROM PAQUETES p WHERE UPPER(p.estado_actual) NOT IN ('ENTREGADO','CANCELADO')
              AND NOT EXISTS (SELECT 1 FROM PARADAS_RUTA pr JOIN TURNOS_REPARTIDOR t ON t.id_turno=pr.id_turno
                  WHERE pr.id_paquete=p.id_paquete AND t.fecha_turno=CONVERT(date,GETDATE())
                    AND UPPER(pr.estado_parada) NOT IN ('ENTREGADA','ENTREGADO','COMPLETADA','COMPLETADO','CANCELADA','CANCELADO'))
            ORDER BY p.es_prioritario DESC,p.folio
            """, (rs, fila) -> new CatalogosRutaDto.PaqueteDisponibleDto(rs.getInt("id_paquete"), rs.getString("folio"), rs.getString("direccion_destino"),
                rs.getBoolean("es_prioritario"), rs.getBoolean("es_fragil"), rs.getString("estado_actual")));
        return new CatalogosRutaDto(repartidores, unidades, paquetes);
    }
}
