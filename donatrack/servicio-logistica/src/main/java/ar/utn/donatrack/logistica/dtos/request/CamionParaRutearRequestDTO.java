package ar.utn.donatrack.logistica.dtos.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Camión tal como lo recibe el proveedor/mock de ruteo en la solicitud síncrona por camión. */
@Getter
@Setter
@NoArgsConstructor
public class CamionParaRutearRequestDTO {
    private UUID id;
    private String patente;
    private double capacidadVolumenM3;
    private double alturaM;
    private double capacidadCargaKg;
}
