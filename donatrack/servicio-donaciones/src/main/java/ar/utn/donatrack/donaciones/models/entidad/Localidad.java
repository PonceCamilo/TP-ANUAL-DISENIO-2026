package ar.utn.donatrack.donaciones.models.entidad;


import lombok.Builder;
import lombok.Getter;

import ar.utn.donatrack.donaciones.models.entidad.Provincia;

/**
 * Objeto de valor que representa una dirección postal.
 * Inmutable: no tiene setters. Si la dirección cambia, se crea una nueva instancia.
 */

@Builder
@Getter
public class Localidad {

    private String nombre;
    private Provincia provincia;
}
