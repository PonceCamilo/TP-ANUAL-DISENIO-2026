package ar.utn.donatrack.donaciones.dtos.response.publico;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una donación entregada, para la landing y el mapa sin login (Entrega 6,
 * requisitos 1 y 2). Lleva qué se donó, cuándo se entregó y a qué entidad, pero
 * nada de la persona donante ni las descripciones que ella escribió.
 */
@Getter
@Builder
public class DonacionEntregadaPublicaDTO {
    private final UUID id;
    private final String subcategoria;
    /** Suma de las cantidades de sus bienes. */
    private final int cantidad;
    /** Unidad del primer bien (los bienes de una donación son de la misma subcategoría). */
    private final String unidad;
    private final LocalDateTime fechaEntrega;
    /** URL de una foto: la de la recepción o, si no hay, la de un bien. Null si no hay ninguna. */
    private final String foto;
    private final UbicacionPublicaDTO entidad;
}
