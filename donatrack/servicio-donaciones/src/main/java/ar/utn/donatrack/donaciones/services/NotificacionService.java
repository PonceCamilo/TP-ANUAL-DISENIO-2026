package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;

/**
 * Servicio mínimo para notificaciones usado por la importación.
 */
public interface NotificacionService {
    void enviarCredencialesNuevoUsuario(PersonaDonante donante);
}
