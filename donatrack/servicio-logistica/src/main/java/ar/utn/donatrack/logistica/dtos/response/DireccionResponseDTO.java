package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DireccionResponseDTO {
    private String calle;
    private int numero;
    private String localidad;
    private String provincia;
    private String codigoPostal;

    public static DireccionResponseDTO desde(Direccion direccion) {
        if (direccion == null) {
            return null;
        }
        return DireccionResponseDTO.builder()
                .calle(direccion.getCalle())
                .numero(direccion.getNumero())
                .localidad(direccion.getLocalidad())
                .provincia(direccion.getProvincia())
                .codigoPostal(direccion.getCodigoPostal())
                .build();
    }
}
