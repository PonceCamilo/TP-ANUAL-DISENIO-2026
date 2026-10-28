package ar.utn.donatrack.donaciones.models.donacion;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad beneficiaria y fecha de la asignación de una donación. Los dos son
 * nulos mientras la donación no está asignada (NINGUNA).
 */
public record Asignacion(UUID idEntidad, LocalDate fecha) {

    public static final Asignacion NINGUNA = new Asignacion(null, null);

    public boolean existe() {
        return idEntidad != null && fecha != null;
    }
}
