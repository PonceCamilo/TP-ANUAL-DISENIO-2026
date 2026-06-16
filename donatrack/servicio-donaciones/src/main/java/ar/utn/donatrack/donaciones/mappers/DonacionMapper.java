package ar.utn.donatrack.donaciones.mappers;

import ar.utn.donatrack.donaciones.dtos.response.BienResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.CambioEstadoResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;

import java.util.List;

public class DonacionMapper {

  private DonacionMapper() {
  }

  public static DonacionResponseDTO toDTO(Donacion donacion) {
    return DonacionResponseDTO.builder()
        .id(donacion.getId())
        .idDonante(donacion.getIdDonante())
        .subcategoria(donacion.getSubcategoria() != null
            ? donacion.getSubcategoria().getTipo() : null)
        .estado(donacion.getEstado())
        .bienes(toBienesDTO(donacion.getBienes()))
        .historialEstados(toHistorialDTO(donacion.getHistorialEstados()))
        .build();
  }

  public static List<DonacionResponseDTO> toDTOList(List<Donacion> donaciones) {
    return donaciones.stream().map(DonacionMapper::toDTO).toList();
  }

  private static List<BienResponseDTO> toBienesDTO(List<Bien> bienes) {
    return bienes.stream().map(DonacionMapper::toBienDTO).toList();
  }

  private static BienResponseDTO toBienDTO(Bien bien) {
    BienResponseDTO.BienResponseDTOBuilder builder = BienResponseDTO.builder()
        .subcategoria(bien.getSubcategoria() != null
            ? bien.getSubcategoria().getTipo() : null)
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

  private static List<CambioEstadoResponseDTO> toHistorialDTO(List<CambioEstado> historial) {
    return historial.stream()
        .map(c -> CambioEstadoResponseDTO.builder()
            .estado(c.getEstado())
            .justificacion(c.getJustificacion())
            .fechaHora(c.getFechaHora())
            .build())
        .toList();
  }
}