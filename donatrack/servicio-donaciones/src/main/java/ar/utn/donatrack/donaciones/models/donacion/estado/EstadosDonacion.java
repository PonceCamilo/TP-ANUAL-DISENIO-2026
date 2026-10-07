package ar.utn.donatrack.donaciones.models.donacion.estado;

import ar.utn.donatrack.donaciones.exceptions.comunes.TipoDesconocidoException;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Reconstruye el objeto de estado a partir de su nombre.
 *
 * POR QUÉ EXISTE: el patrón State está modelado con clases, no con un enum, y
 * esos objetos NO se persisten. En la base se guarda solamente el nombre del
 * estado ("EN_DEPOSITO", "ENTREGADA", ...) y al cargar la donación se vuelve a
 * instanciar la clase correspondiente.
 *
 * Es la contracara del mapeo: `Donacion.estado` es @Transient y se rehidrata en
 * @PostLoad con este factory. Así la base guarda un dato simple y legible —que
 * además queda bien en el DER— sin perder el comportamiento del patrón.
 */
public final class EstadosDonacion {

    private static final Map<String, Supplier<EstadoDonacionBase>> POR_NOMBRE = Map.of(
            "EN_DEPOSITO", EnDepositoState::new,
            "ASIGNACION_REALIZADA", AsignacionRealizadaState::new,
            "LISTA_PARA_ENTREGAR", ListaParaEntregarState::new,
            "EN_TRASLADO", EnTrasladoState::new,
            "ENTREGADA", EntregadaState::new,
            "ENTREGA_FALLIDA", EntregaFallidaState::new,
            "VENCIDA", VencidaState::new);

    private EstadosDonacion() {
    }

    /** Estado inicial de toda donación recién registrada. */
    public static EstadoDonacionBase inicial() {
        return new EnDepositoState();
    }

    public static EstadoDonacionBase desde(String nombre) {
        Supplier<EstadoDonacionBase> factory = POR_NOMBRE.get(nombre);
        if (factory == null) {
            throw new TipoDesconocidoException("estado de donación '" + nombre + "'");
        }
        return factory.get();
    }

    /** Tolerante a null: si la columna está vacía devuelve null en lugar de fallar. */
    public static EstadoDonacionBase desdeONull(String nombre) {
        return nombre == null ? null : desde(nombre);
    }
}
