package ar.utn.donatrack.donaciones.models.donante.estado;

import ar.utn.donatrack.donaciones.models.estado.CatalogoDeEstados;

import java.util.Map;

/**
 * Los nombres de los estados de una persona donante y el catálogo que
 * reconstruye el objeto de estado a partir del nombre guardado. Mismo criterio
 * que EstadosDonacion.
 */
public final class EstadosDonante {

    public static final String ACTIVO = "ACTIVO";
    public static final String INACTIVO = "INACTIVO";
    public static final String BLOQUEADO = "BLOQUEADO";

    private static final CatalogoDeEstados<EstadoDonante> CATALOGO = new CatalogoDeEstados<>("donante", Map.of(
            ACTIVO, ActivoState::new,
            INACTIVO, InactivoState::new,
            BLOQUEADO, BloqueadoState::new));

    private EstadosDonante() {
    }

    /** Todo donante nuevo —registrado a mano o importado por CSV— arranca ACTIVO. */
    public static EstadoDonante inicial() {
        return new ActivoState();
    }

    public static EstadoDonante desde(String nombre) {
        return CATALOGO.desde(nombre);
    }
}
