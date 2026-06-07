package ar.utn.donatrack.donaciones.models.entidad;


import lombok.Builder;
import lombok.Getter;

/**
 * Objeto de valor que representa una dirección postal.
 * Inmutable: no tiene setters. Si la dirección cambia, se crea una nueva instancia.
 */

@Builder
@Getter
public class Provincia {

    private String nombre;
}
