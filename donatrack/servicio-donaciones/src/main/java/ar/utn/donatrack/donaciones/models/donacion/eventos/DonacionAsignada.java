package ar.utn.donatrack.donaciones.models.donacion.eventos;

import java.util.UUID;

/** Una persona administradora confirmó la entidad destino de la donación. */
public record DonacionAsignada(UUID idDonacion, UUID idDonante, UUID idEntidad) {
}
