package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.BienResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.CambioEstadoResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
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
        .bienes(toBienesDTO(donacion.getBienes()))
        .historialEstados(toHistorialDTO(donacion.getHistorialEstados()))
        .build();
  }

  public List<DonacionResponseDTO> toDTOList(List<Donacion> donaciones) {
    return donaciones.stream().map(this::toDTO).toList();
  }

  public Bien toBien(BienRequestDTO dto) {
    if (dto.getFechaVencimiento() != null) {
      return BienPerecible.builder()
          .subcategoria(new Subcategoria(dto.getSubcategoria()))
          .descripcion(dto.getDescripcion())
          .foto(dto.getFoto())
          .cantidad(dto.getCantidad())
          .unidad(dto.getUnidad())
          .fechaVencimiento(dto.getFechaVencimiento())
          .build();
    }
    if (dto.getEsNuevo() != null) {
      return BienConEstado.builder()
          .subcategoria(new Subcategoria(dto.getSubcategoria()))
          .descripcion(dto.getDescripcion())
          .foto(dto.getFoto())
          .cantidad(dto.getCantidad())
          .unidad(dto.getUnidad())
          .esNuevo(dto.getEsNuevo())
          .build();
    }
    return BienGenerico.builder()
        .subcategoria(new Subcategoria(dto.getSubcategoria()))
        .descripcion(dto.getDescripcion())
        .foto(dto.getFoto())
        .cantidad(dto.getCantidad())
        .unidad(dto.getUnidad())
        .build();
  }

  private List<BienResponseDTO> toBienesDTO(List<Bien> bienes) {
    return bienes.stream().map(this::toBienDTO).toList();
  }

  private BienResponseDTO toBienDTO(Bien bien) {
    BienResponseDTO.BienResponseDTOBuilder builder = BienResponseDTO.builder()
        .subcategoria(bien.getSubcategoria() != null ? bien.getSubcategoria().getTipo() : null)
        .descripcion(bien.getDescripcion())
        .foto(bien.getFoto())
        .cantidad(bien.getCantidad())
        .unidad(bien.getUnidad());

    if (bien instanceof BienConEstado bienConEstado) {
      builder.esNuevo(bienConEstado.isEsNuevo());
    }
    if (bien instanceof BienPerecible bienPerecible) {
      builder.fechaVencimiento(bienPerecible.getFechaVencimiento());
    }

    return builder.build();
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
