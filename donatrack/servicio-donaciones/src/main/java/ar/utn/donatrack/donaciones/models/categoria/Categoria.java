package ar.utn.donatrack.donaciones.models.categoria;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Categoría de bien donado (ej: Alimentos, Vestimenta, Mobiliario).
 *
 * El segmentador usa estas flags para decidir el tipo de Donacion a generar.
 */

@Builder
@Getter
@Setter
public class Categoria {

    private UUID id;
    private String nombre;
    private TipoBien tipoBien;

    @Builder.Default
    private List<Subcategoria> subcategorias = new ArrayList<>();
}
