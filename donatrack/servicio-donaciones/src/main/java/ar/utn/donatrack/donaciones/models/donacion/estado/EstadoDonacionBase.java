package ar.utn.donatrack.donaciones.models.donacion.estado;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.CambioEstadoDonacionIlegalException;
import java.util.Map;
import java.util.function.Supplier;

public abstract class EstadoDonacionBase {

    private final String nombre;
    private final Map<String, Supplier<EstadoDonacionBase>> transiciones;

    protected EstadoDonacionBase(String nombre, Map<String, Supplier<EstadoDonacionBase>> transiciones) {
        this.nombre = nombre;
        this.transiciones = transiciones;
    }

    public EstadoDonacionBase transicionarA(String estadoDestino, String justificacion) {
        Supplier<EstadoDonacionBase> factory = transiciones.get(estadoDestino);
        if (factory == null) {
            throw new CambioEstadoDonacionIlegalException(nombre(), estadoDestino);
        }
        return factory.get();
    }

    public String nombre() {
        return nombre;
    }

}
