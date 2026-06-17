package ar.utn.donatrack.notificaciones.interfaces.services;

import ar.utn.donatrack.notificaciones.model.Notificacion;

public interface EstrategiaEnvioInterface {
    void enviar(Notificacion notificacion);
}