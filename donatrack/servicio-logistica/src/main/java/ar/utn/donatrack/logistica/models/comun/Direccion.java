package ar.utn.donatrack.logistica.models.comun;

import lombok.Builder;
import lombok.Getter;

/**
 * Objeto de valor propio de logística (bounded context independiente):
 * no reutiliza la Direccion de servicio-donaciones para no acoplarse
 * a un modelo ajeno. Inmutable: si la dirección cambia, se crea otra instancia.
 */
@Builder
@Getter
public class Direccion {
    private String calle;
    private int numero;
    private String localidad;
    private String provincia;
    private String codigoPostal;
}
