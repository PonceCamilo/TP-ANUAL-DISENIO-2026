package ar.utn.donatrack.donaciones.models.donante.estado;

import ar.utn.donatrack.donaciones.exceptions.personasExceptions.CambioEstadoPersonaIlegalException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;

import java.util.Map;
import java.util.function.Supplier;

public abstract class EstadoDonante {

    private final String nombre;
    private final Map<String, Supplier<EstadoDonante>> transiciones;

    protected EstadoDonante(String nombre, Map<String, Supplier<EstadoDonante>> transiciones) {
        this.nombre = nombre;
        this.transiciones = transiciones;
    }

    public EstadoDonante transicionarA(String estadoDestino, String justificacion) {
        if (nombre.equals(estadoDestino)) {
            throw new PersonaConMismoEstadoException(estadoDestino);
        }
        Supplier<EstadoDonante> factory = transiciones.get(estadoDestino);
        if (factory == null) {
            throw new CambioEstadoPersonaIlegalException(nombre, estadoDestino);
        }
        return factory.get();
    }

    public String nombre() {
        return nombre;
    }

}
