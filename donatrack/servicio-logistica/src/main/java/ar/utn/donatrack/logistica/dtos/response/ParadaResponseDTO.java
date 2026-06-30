package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.planificacion.Parada;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ParadaResponseDTO {
    private UUID id;
    private int orden;
    private DireccionResponseDTO direccion;
    private UUID idEntidadBeneficiaria;
    private List<UUID> entregasIds;

    public static ParadaResponseDTO desde(Parada parada) {
        return ParadaResponseDTO.builder()
                .id(parada.getId())
                .orden(parada.getOrden())
                .direccion(DireccionResponseDTO.desde(parada.getDireccion()))
                .idEntidadBeneficiaria(parada.getIdEntidadBeneficiaria())
                .entregasIds(parada.getEntregasIds())
                .build();
    }
}
