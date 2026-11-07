package ar.utn.donatrack.logistica.dtos.response;

import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CamionResponseDTO {
    private UUID id;
    private String patente;
    private double capacidadVolumenM3;
    private double alturaM;
    private double capacidadCargaKg;
    private EstadoCamion estado;
    private Double latitud;
    private Double longitud;
    private LocalDateTime ubicacionActualizadaEn;

    public static CamionResponseDTO desde(Camion camion) {
        return CamionResponseDTO.builder()
                .id(camion.getId())
                .patente(camion.getPatente())
                .capacidadVolumenM3(camion.getCapacidadVolumenM3())
                .alturaM(camion.getAlturaM())
                .capacidadCargaKg(camion.getCapacidadCargaKg())
                .estado(camion.getEstado())
                .latitud(camion.getLatitud())
                .longitud(camion.getLongitud())
                .ubicacionActualizadaEn(camion.getUbicacionActualizadaEn())
                .build();
    }
}
