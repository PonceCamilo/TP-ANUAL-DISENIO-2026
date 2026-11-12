package ar.utn.donatrack.donaciones.dtos.response.publico;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Una entidad beneficiaria tal como se muestra sin login (Entrega 6): nombre y
 * zona, más las coordenadas para el mapa si las tiene. Sin calle ni número, sin
 * contactos ni representantes: esos datos no se publican.
 */
@Getter
@Builder
public class UbicacionPublicaDTO {
    private final UUID idEntidad;
    private final String razonSocial;
    private final String localidad;
    private final String provincia;
    /** Null si la entidad no cargó su ubicación. */
    private final Double latitud;
    private final Double longitud;
}
