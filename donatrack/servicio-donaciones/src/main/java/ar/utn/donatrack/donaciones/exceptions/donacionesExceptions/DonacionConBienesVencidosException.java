package ar.utn.donatrack.donaciones.exceptions.donacionesExceptions;

import ar.utn.donatrack.donaciones.exceptions.ReglaDeNegocioException;

import java.util.UUID;

/**
 * La donación tiene bienes perecederos ya vencidos: no se le buscan candidatas ni
 * se asigna. El enunciado deja en manos de la persona administradora pasarla a
 * VENCIDA; el mensaje se lo indica.
 */
public class DonacionConBienesVencidosException extends ReglaDeNegocioException {

    public DonacionConBienesVencidosException(UUID idDonacion) {
        super("La donación " + idDonacion + " tiene bienes vencidos: hay que marcarla como VENCIDA "
                + "(PATCH /donaciones/" + idDonacion + "/estado) en lugar de asignarla.");
    }
}
