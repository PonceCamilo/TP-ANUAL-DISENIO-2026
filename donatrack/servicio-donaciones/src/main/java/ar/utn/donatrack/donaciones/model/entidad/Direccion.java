package ar.utn.donatrack.donaciones.model.entidad;
import lombok.Getter;
import lombok.Setter;
/**
 * Objeto de valor que representa una dirección postal.
 * Inmutable: no tiene setters. Si la dirección cambia, se crea una nueva instancia.
 */

@Getter
@Setter
public class Direccion {

    private String calle;
    private int numero;
    private String localidad;
    private String provincia;
    private String codigoPostal;

    public Direccion(String calle, int numero, String localidad,
                     String provincia, String codigoPostal) {
        this.calle = calle;
        this.numero = numero;
        this.localidad = localidad;
        this.provincia = provincia;
        this.codigoPostal = codigoPostal;
    }

}
