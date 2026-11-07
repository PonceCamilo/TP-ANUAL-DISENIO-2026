package ar.utn.donatrack.donaciones.salida;

import java.util.Set;

/**
 * Sabe mandar los mensajes de uno o más destinos de la bandeja de salida
 * (patrón Strategy: RelayDeMensajes elige el enviador por el destino).
 */
public interface EnviadorDeMensajes {

    Set<String> destinos();

    /** Envía el mensaje (JSON). Si no pudo, lanza la excepción: el relay lo reintenta. */
    void enviar(String destino, String payload) throws Exception;
}
