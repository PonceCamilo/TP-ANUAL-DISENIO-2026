package ar.utn.donatrack.donaciones.models.estado;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Base común del patrón State de donaciones y de personas donantes.
 *
 * Cada estado es una clase que declara su nombre y a qué estados puede pasar; la
 * base pone el mecanismo una sola vez: buscar el destino entre las transiciones
 * permitidas y rechazar las demás. Antes había dos jerarquías con el mismo código
 * copiado (EstadoDonacionBase y EstadoDonante).
 *
 * transicionarA es un Template Method: un estado que necesita una regla propia
 * antes de cambiar (por ejemplo, exigir una justificación) redefine
 * validarTransicion y no el mecanismo entero.
 *
 * Los estados no guardan datos: se crean al vuelo a partir del nombre, que es lo
 * único que se persiste.
 *
 * @param <E> la jerarquía concreta, para que transicionarA devuelva su mismo tipo
 */
public abstract class Estado<E extends Estado<E>> {

    private final String nombre;
    private final Map<String, Supplier<E>> transiciones;

    protected Estado(String nombre, Map<String, Supplier<E>> transiciones) {
        this.nombre = nombre;
        this.transiciones = Map.copyOf(transiciones);
    }

    public final E transicionarA(String estadoDestino, String justificacion) {
        validarTransicion(estadoDestino, justificacion);
        Supplier<E> destino = transiciones.get(estadoDestino);
        if (destino == null) {
            throw new TransicionDeEstadoIlegalException(nombre, estadoDestino);
        }
        return destino.get();
    }

    /** Regla propia de un estado antes de dejarlo. Por defecto no exige nada. */
    protected void validarTransicion(String estadoDestino, String justificacion) {
    }

    public String nombre() {
        return nombre;
    }

    public boolean es(String nombreEstado) {
        return nombre.equals(nombreEstado);
    }

    protected static boolean sinJustificacion(String justificacion) {
        return justificacion == null || justificacion.isBlank();
    }

    /** Dos estados son iguales si son el mismo estado: no tienen otros datos. */
    @Override
    public boolean equals(Object otro) {
        return otro != null && getClass() == otro.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return nombre;
    }
}
