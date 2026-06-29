package ar.utn.donatrack.incentivos.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EvolucionPeriodo {
    private LocalDate fecha;
    private int cantidadBienes;
    private List<String> categoriasDonadas;
    private boolean exitosa;

}
