package ar.utn.donatrack.donaciones.models.donacion.estado;

import ar.utn.donatrack.donaciones.models.estado.CatalogoDeEstados;

import java.util.Map;

/**
 * Los nombres de los estados de una donación y el catálogo que reconstruye el
 * objeto de estado a partir del nombre guardado.
 *
 * POR QUÉ EXISTE: el patrón State está modelado con clases, no con un enum, y
 * esos objetos NO se persisten. En la base se guarda solamente el nombre del
 * estado y Donacion.getEstado() arma el objeto al vuelo con este catálogo.
 *
 * Las constantes evitan escribir los nombres como texto suelto en services y
 * modelos: un error de tipeo pasa a ser un error de compilación.
 */
public final class EstadosDonacion {

    public static final String EN_DEPOSITO = "EN_DEPOSITO";
    public static final String ASIGNACION_REALIZADA = "ASIGNACION_REALIZADA";
    public static final String LISTA_PARA_ENTREGAR = "LISTA_PARA_ENTREGAR";
    public static final String EN_TRASLADO = "EN_TRASLADO";
    public static final String ENTREGADA = "ENTREGADA";
    public static final String ENTREGA_FALLIDA = "ENTREGA_FALLIDA";
    public static final String VENCIDA = "VENCIDA";

    private static final CatalogoDeEstados<EstadoDonacionBase> CATALOGO = new CatalogoDeEstados<>("donación", Map.of(
            EN_DEPOSITO, EnDepositoState::new,
            ASIGNACION_REALIZADA, AsignacionRealizadaState::new,
            LISTA_PARA_ENTREGAR, ListaParaEntregarState::new,
            EN_TRASLADO, EnTrasladoState::new,
            ENTREGADA, EntregadaState::new,
            ENTREGA_FALLIDA, EntregaFallidaState::new,
            VENCIDA, VencidaState::new));

    private EstadosDonacion() {
    }

    /** Estado inicial de toda donación recién registrada. */
    public static EstadoDonacionBase inicial() {
        return new EnDepositoState();
    }

    public static EstadoDonacionBase desde(String nombre) {
        return CATALOGO.desde(nombre);
    }

    /** Tolerante a null: si la columna está vacía devuelve null en lugar de fallar. */
    public static EstadoDonacionBase desdeONull(String nombre) {
        return nombre == null ? null : desde(nombre);
    }
}
