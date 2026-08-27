package ar.utn.donatrack.incentivos.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class EvolucionPeriodo {
    private int mes;
    private int anio;
    private int cantidadDonaciones;

    public void incrementarDonaciones() {
        this.cantidadDonaciones++;
    }
}
