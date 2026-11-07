package ar.utn.donatrack.donaciones.models.donacion.eventos;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * La donación llegó a la entidad. Lleva lo que necesitan los oyentes para no
 * volver a leerla: la subcategoría y la cantidad que se imputan a las
 * necesidades, y la fecha de entrega que se informa a incentivos.
 */
public record DonacionEntregada(UUID idDonacion, UUID idDonante, UUID idEntidad,
                                Subcategoria subcategoria, int cantidad, LocalDateTime fechaEntrega) {
}
