package ar.utn.donatrack.logistica.exceptions;

import java.util.UUID;

/** Se lanza cuando el callback del proveedor externo no trae el token de correlación esperado para el lote. */
public class LoteCallbackInvalidoException extends RuntimeException {
    public LoteCallbackInvalidoException(UUID loteId) {
        super("El token de correlación del callback no coincide con el lote " + loteId);
    }
}
