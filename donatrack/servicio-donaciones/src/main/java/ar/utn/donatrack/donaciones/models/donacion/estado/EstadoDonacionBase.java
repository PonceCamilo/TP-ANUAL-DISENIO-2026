package ar.utn.donatrack.donaciones.models.donacion.estado;

import ar.utn.donatrack.donaciones.models.estado.Estado;

import java.util.Map;
import java.util.function.Supplier;

/** Raíz de los estados de una donación (ver Estado para el mecanismo común). */
public abstract class EstadoDonacionBase extends Estado<EstadoDonacionBase> {

    protected EstadoDonacionBase(String nombre, Map<String, Supplier<EstadoDonacionBase>> transiciones) {
        super(nombre, transiciones);
    }
}
