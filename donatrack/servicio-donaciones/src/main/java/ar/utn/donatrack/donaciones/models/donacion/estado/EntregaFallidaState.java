package ar.utn.donatrack.donaciones.models.donacion.estado;

import java.util.Map;

/**
 * La entrega no pudo concretarse.
 *
 * Dos salidas posibles:
 *   - EN_DEPOSITO: la donación vuelve al depósito para replanificarse desde cero.
 *   - EN_TRASLADO: el proveedor de logística reintenta la entrega sin que la
 *     donación pase por el depósito.
 *
 * POR QUÉ EXISTE EL REINTENTO DIRECTO: los dos proveedores pueden volver a
 * intentar una entrega fallida (el externo permite FALLIDO → EN_CAMINO). Cuando
 * eso pasa, llega otro inicio-ruta para una donación que está en ENTREGA_FALLIDA.
 * Sin esta transición, el callback fallaría con una transición ilegal, Donaciones
 * respondería 400, el broker devolvería 502 al proveedor y la donación quedaría
 * desincronizada respecto de lo que el proveedor cree que está pasando.
 *
 * Se eligió esta opción sobre la alternativa de encadenar
 * EN_DEPOSITO → ASIGNACION_REALIZADA → LISTA_PARA_ENTREGAR automáticamente:
 * esa ruta usa transiciones que ya existen, pero deja tres pasos falsos en el
 * historial por cada reintento y arruina la trazabilidad que pide el enunciado.
 */
public class EntregaFallidaState extends EstadoDonacionBase {

    public EntregaFallidaState() {
        super("ENTREGA_FALLIDA", Map.of(
            "EN_DEPOSITO", EnDepositoState::new,
            "EN_TRASLADO", EnTrasladoState::new
        ));
    }

}
