package ar.utn.donatrack.logistica.models.comun;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Valor embebido. No tiene colección ni id: vive dentro de la parada
 * o de la donación del lote que la usa. Si la dirección cambia, se crea otra.
 */
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Direccion {
    private String calle;
    private int numero;
    private String localidad;
    private String provincia;
    private String codigoPostal;
}
