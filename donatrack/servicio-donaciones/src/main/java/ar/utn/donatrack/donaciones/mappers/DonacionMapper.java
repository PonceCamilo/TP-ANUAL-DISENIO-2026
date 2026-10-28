package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.DonacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.BienResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.CambioEstadoResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DonacionMapper {

    public DonacionResponseDTO toDTO(Donacion donacion) {
        return DonacionResponseDTO.builder()
                .id(donacion.getId())
                .idDonante(donacion.getIdDonante())
                .idEntidadBeneficiaria(donacion.getIdEntidadBeneficiaria())
                .descripcion(donacion.getDescripcion())
                .fechaDonacion(donacion.getFechaDonacion())
                .subcategoria(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getTipo() : null)
                .estado(donacion.getEstado().nombre())
                .proveedorLogistica(donacion.getProveedorLogistica())
                .idSeguimientoLogistica(donacion.getIdSeguimientoLogistica())
                .bienes(toBienesDTO(donacion.getBienes()))
                .historialEstados(toHistorialDTO(donacion.getHistorialEstados()))
                .build();
    }

    public List<DonacionResponseDTO> toDTOList(List<Donacion> donaciones) {
        return donaciones.stream().map(this::toDTO).toList();
    }

    public CargaDonacion toCargaDonacion(DonacionRequestDTO dto) {
        return new CargaDonacion(
                dto.getIdDonante(),
                dto.getDescripcion(),
                dto.getBienes().stream().map(this::toBien).toList()
        );
    }

    /** Arma la subclase que corresponde al tipo del bien (indicado o deducido, ver BienRequestDTO). */
    public Bien toBien(BienRequestDTO dto) {
        return switch (dto.tipoResuelto()) {
            case PERECIBLE -> conDatosComunes(BienPerecible.builder(), dto)
                    .fechaVencimiento(dto.getFechaVencimiento())
                    .build();
            case CON_ESTADO -> conDatosComunes(BienConEstado.builder(), dto)
                    .esNuevo(dto.getEsNuevo())
                    .build();
            case GENERICO -> conDatosComunes(BienGenerico.builder(), dto).build();
        };
    }

    private static <C extends Bien, B extends Bien.BienBuilder<C, B>> B conDatosComunes(
            Bien.BienBuilder<C, B> builder, BienRequestDTO dto) {
        return builder
                .subcategoria(new Subcategoria(dto.getSubcategoria()))
                .descripcion(dto.getDescripcion())
                .foto(dto.getFoto())
                .cantidad(dto.getCantidad())
                .unidad(dto.getUnidad());
    }

    private List<BienResponseDTO> toBienesDTO(List<Bien> bienes) {
        return bienes.stream().map(this::toBienDTO).toList();
    }

    private BienResponseDTO toBienDTO(Bien bien) {
        return BienResponseDTO.builder()
                .id(bien.getId())
                .tipo(bien.tipo())
                .subcategoria(bien.getSubcategoria() != null ? bien.getSubcategoria().getTipo() : null)
                .descripcion(bien.getDescripcion())
                .foto(bien.getFoto())
                .cantidad(bien.getCantidad())
                .unidad(bien.getUnidad())
                .esNuevo(bien.nuevo().orElse(null))
                .fechaVencimiento(bien.vencimiento().orElse(null))
                .build();
    }

    private List<CambioEstadoResponseDTO> toHistorialDTO(List<CambioEstado> historial) {
        return historial.stream()
                .map(c -> CambioEstadoResponseDTO.builder()
                        .estadoPrevio(c.getEstadoPrevio() != null ? c.getEstadoPrevio().nombre() : null)
                        .estado(c.getEstado().nombre())
                        .nombreTransicion(c.getNombreTransicion())
                        .justificacion(c.getJustificacion())
                        .fechaHora(c.getFechaHora())
                        .build())
                .toList();
    }
}
