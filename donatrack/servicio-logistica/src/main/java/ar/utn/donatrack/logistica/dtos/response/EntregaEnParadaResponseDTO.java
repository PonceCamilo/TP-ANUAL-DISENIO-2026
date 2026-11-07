package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.entrega.Entrega;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** Entrega de una parada, con la donación a la que corresponde. */
@Getter
@Builder
public class EntregaEnParadaResponseDTO {
    private UUID id;
    private UUID idDonacion;

    public static EntregaEnParadaResponseDTO desde(Entrega entrega) {
        return EntregaEnParadaResponseDTO.builder()
                .id(entrega.getId())
                .idDonacion(entrega.getIdDonacion())
                .build();
    }
}
