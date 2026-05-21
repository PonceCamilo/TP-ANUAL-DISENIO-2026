package ar.utn.donatrack.donaciones.model.entidad;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
/**
 * Objeto de valor que representa una dirección postal.
 * Inmutable: no tiene setters. Si la dirección cambia, se crea una nueva instancia.
 */

@Builder
@Getter
@Setter
public class Direccion {

    private String calle;
    private int numero;
    private String localidad;
    private String provincia;
    private String codigoPostal;
}
