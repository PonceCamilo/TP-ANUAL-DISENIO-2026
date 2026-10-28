package ar.utn.donatrack.donaciones.models.donante.estado;

import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import ar.utn.donatrack.donaciones.models.estado.Estado;

import java.util.Map;
import java.util.function.Supplier;

/** Raíz de los estados de una persona donante (ver Estado para el mecanismo común). */
public abstract class EstadoDonante extends Estado<EstadoDonante> {

    protected EstadoDonante(String nombre, Map<String, Supplier<EstadoDonante>> transiciones) {
        super(nombre, transiciones);
    }

    /** Pedir el estado que la persona ya tiene es un error propio (409), no una transición ilegal. */
    @Override
    protected void validarTransicion(String estadoDestino, String justificacion) {
        if (es(estadoDestino)) {
            throw new PersonaConMismoEstadoException(estadoDestino);
        }
    }
}
