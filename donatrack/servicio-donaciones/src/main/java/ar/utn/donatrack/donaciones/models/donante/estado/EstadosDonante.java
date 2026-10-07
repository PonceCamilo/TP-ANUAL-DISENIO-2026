package ar.utn.donatrack.donaciones.models.donante.estado;

import ar.utn.donatrack.donaciones.exceptions.comunes.TipoDesconocidoException;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Reconstruye el estado de una persona donante a partir de su nombre.
 *
 * Mismo criterio que EstadosDonacion: en la base se guarda el nombre
 * ("ACTIVO", "INACTIVO", "BLOQUEADO") y el objeto de estado se rehidrata al
 * cargar la persona.
 */
public final class EstadosDonante {

    private static final Map<String, Supplier<EstadoDonante>> POR_NOMBRE = Map.of(
            "ACTIVO", ActivoState::new,
            "INACTIVO", InactivoState::new,
            "BLOQUEADO", BloqueadoState::new);

    private EstadosDonante() {
    }

    /** Todo donante nuevo —registrado a mano o importado por CSV— arranca ACTIVO. */
    public static EstadoDonante inicial() {
        return new ActivoState();
    }

    public static EstadoDonante desde(String nombre) {
        Supplier<EstadoDonante> factory = POR_NOMBRE.get(nombre);
        if (factory == null) {
            throw new TipoDesconocidoException("estado de donante '" + nombre + "'");
        }
        return factory.get();
    }
}
