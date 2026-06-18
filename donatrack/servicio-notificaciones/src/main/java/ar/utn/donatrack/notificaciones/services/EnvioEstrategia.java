package ar.utn.donatrack.notificaciones.services;

import ar.utn.donatrack.notificaciones.interfaces.services.EstrategiaEnvioInterface;
import ar.utn.donatrack.notificaciones.model.Notificacion;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase base para las estrategias de envío (patrón Strategy).
 * Centraliza el log de simulación de envío; cada subclase solo
 * define su propio prefijo identificador del medio.
 */
@Slf4j
public abstract class EnvioEstrategia implements EstrategiaEnvioInterface {

    @Override
    public void enviar(Notificacion notificacion) {
        log.info("[{}] Para: {} | Mensaje: {}",
                getPrefijo(), notificacion.getDestinatario(), notificacion.getMensaje());
    }

    protected abstract String getPrefijo();
}