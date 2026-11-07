package ar.utn.donatrack.donaciones.dtos.response;

import java.util.List;

/**
 * El ranking de un algoritmo de asignación: su nombre y las entidades
 * candidatas, de mejor a peor. Un algoritmo nuevo aparece como un elemento más
 * de la lista, sin cambiar el DTO.
 */
public record RankingAlgoritmoResponseDTO(String algoritmo, List<EntidadBeneficiariaResponseDTO> entidades) {
}
