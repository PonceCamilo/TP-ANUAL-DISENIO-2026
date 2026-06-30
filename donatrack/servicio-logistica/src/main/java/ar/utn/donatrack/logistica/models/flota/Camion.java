package ar.utn.donatrack.logistica.models.flota;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class Camion {
    private UUID id;
    private String patente;
    private double capacidadVolumenM3;
    private double alturaM;
    private double capacidadCargaKg;

    @Builder.Default
    private EstadoCamion estado = EstadoCamion.DISPONIBLE;
}
