package ar.utn.donatrack.donaciones.model.donacion;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

import java.time.LocalDate;

public class Donacion {
    // Resultado de la segmentacion de una CargaDonacion
    // TODO: id, subcategoria, cantidad, unidad, estado, cargaOrigen
    // TODO: fechaVencimiento (nullable), esNuevo (nullable)
    private Subcategoria subcategoria;
    private EstadoDonacion estado;
    private CargaDonacion cargaOrigen;
}
