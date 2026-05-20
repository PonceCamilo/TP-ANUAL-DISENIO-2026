package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;

/**
 * Servicio mínimo para notificaciones usado por la importación.
 */
public interface NotificacionServiceInterface {
    void enviarCredencialesNuevoUsuario(PersonaDonante donante);
}
