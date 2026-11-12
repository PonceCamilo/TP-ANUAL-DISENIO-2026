package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.response.publico.DonacionEntregadaPublicaDTO;
import ar.utn.donatrack.donaciones.dtos.response.publico.EntidadPublicaDTO;
import ar.utn.donatrack.donaciones.dtos.response.publico.UbicacionPublicaDTO;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Modelo → datos públicos (Entrega 6). Elige qué se publica: la entidad con su
 * nombre y su zona, y de la donación lo que se dio y cuándo. Nunca la persona
 * donante, ni los contactos o representantes de la entidad, ni su calle.
 */
@Component
public class PublicoMapper {

    public UbicacionPublicaDTO ubicacion(EntidadBeneficiaria entidad) {
        Direccion direccion = entidad.getDireccion();
        boolean conLocalidad = direccion != null && direccion.getLocalidad() != null;
        return UbicacionPublicaDTO.builder()
                .idEntidad(entidad.getId())
                .razonSocial(entidad.getRazonSocial())
                .localidad(conLocalidad ? direccion.getLocalidad().getNombre() : null)
                .provincia(conLocalidad && direccion.getLocalidad().getProvincia() != null
                        ? direccion.getLocalidad().getProvincia().getNombre() : null)
                .latitud(direccion != null && direccion.tieneCoordenadas() ? direccion.getLatitud() : null)
                .longitud(direccion != null && direccion.tieneCoordenadas() ? direccion.getLongitud() : null)
                .build();
    }

    /** @param entidad la entidad a la que se entregó; null si ya no existe */
    public DonacionEntregadaPublicaDTO entregada(Donacion donacion, EntidadBeneficiaria entidad) {
        return DonacionEntregadaPublicaDTO.builder()
                .id(donacion.getId())
                .subcategoria(donacion.getSubcategoria() == null ? null : donacion.getSubcategoria().getTipo())
                .cantidad(donacion.cantidadTotal())
                .unidad(donacion.getBienes().stream().map(Bien::getUnidad).findFirst().orElse(null))
                .fechaEntrega(donacion.getFechaEntrega())
                .foto(donacion.fotoParaMostrar().orElse(null))
                .entidad(entidad == null ? null : ubicacion(entidad))
                .build();
    }

    /** Las necesidades de campañas vigentes a esa fecha que todavía no se completaron. */
    public EntidadPublicaDTO entidad(EntidadBeneficiaria entidad, LocalDate hoy) {
        List<EntidadPublicaDTO.NecesidadAbierta> abiertas = entidad.getCampanias().stream()
                .filter(campania -> campania.estaVigenteAl(hoy))
                .flatMap(campania -> campania.getNecesidades().stream()
                        .filter(necesidad -> !necesidad.estaSatisfechaAl(hoy))
                        .map(necesidad -> necesidadAbierta(campania, necesidad, hoy)))
                .toList();
        return EntidadPublicaDTO.builder()
                .ubicacion(ubicacion(entidad))
                .necesidadesAbiertas(abiertas)
                .build();
    }

    private EntidadPublicaDTO.NecesidadAbierta necesidadAbierta(Campania campania, Necesidad necesidad, LocalDate hoy) {
        return EntidadPublicaDTO.NecesidadAbierta.builder()
                .nombre(necesidad.getNombre())
                .descripcion(necesidad.getDescripcion())
                .subcategoria(necesidad.getSubcategoria() == null ? null : necesidad.getSubcategoria().getTipo())
                .cantidadObjetivo(necesidad.getCantidadObjetivo())
                .cantidadRecibida(necesidad.cantidadRecibidaAl(hoy))
                .campania(campania.getDescripcionGeneral())
                .finDeCampania(campania.getFechaFin())
                .build();
    }
}
