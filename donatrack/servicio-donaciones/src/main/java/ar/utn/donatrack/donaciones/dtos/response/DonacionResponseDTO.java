package ar.utn.donatrack.donaciones.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DonacionResponseDTO {
  private UUID id;
  private UUID idDonante;
  private UUID idEntidadBeneficiaria;
  private String descripcion;
  private LocalDateTime fechaDonacion;
  private String subcategoria;
  private String estado;
  private List<BienResponseDTO> bienes;
  private List<CambioEstadoResponseDTO> historialEstados;
}