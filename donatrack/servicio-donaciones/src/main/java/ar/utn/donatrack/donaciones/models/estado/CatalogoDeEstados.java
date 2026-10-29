package ar.utn.donatrack.donaciones.models.estado;

import ar.utn.donatrack.donaciones.exceptions.comunes.EstadoDesconocidoException;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Reconstruye el objeto de estado a partir del nombre guardado en la base.
 *
 * Es la contracara del mapeo: la base guarda un dato simple y legible (que además
 * queda bien en el DER) y el comportamiento del patrón se recupera al leerlo. Un
 * nombre que no está en el catálogo es un dato corrupto en la base, no un error
 * del cliente: EstadoDesconocidoException responde 500.
 */
public final class CatalogoDeEstados<E extends Estado<E>> {

    private final String deQue;
    private final Map<String, Supplier<E>> porNombre;

    /** @param deQue a qué pertenecen los estados ("donación", "donante"), para el mensaje de error */
    public CatalogoDeEstados(String deQue, Map<String, Supplier<E>> porNombre) {
        this.deQue = deQue;
        this.porNombre = Map.copyOf(porNombre);
    }

    public E desde(String nombre) {
        Supplier<E> estado = nombre == null ? null : porNombre.get(nombre);
        if (estado == null) {
            throw new EstadoDesconocidoException(deQue, nombre);
        }
        return estado.get();
    }
}
