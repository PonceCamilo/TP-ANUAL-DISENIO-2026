package ar.utn.donatrack.incentivos.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder

public class EvolucionPeriodo {
    private int mes;
    private int anio;
    private int cantidadDonaciones;
}