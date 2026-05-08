package ar.utn.donatrack.donaciones.model.donacion;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Donacion {
    // Resultado de la segmentacion de una CargaDonacion
    // TODO: id, subcategoria, cantidad, unidad, estado, cargaOrigen
    // TODO: fechaVencimiento (nullable), esNuevo (nullable)
    private Subcategoria subcategoria;
    private EstadoDonacion estado;
    private CargaDonacion cargaOrigen;
    private LocalDate fechaVencimiento; // Solo para perecibles
    private boolean esNuevo; // Solo para bienes con estado
}
