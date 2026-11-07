package ar.utn.donatrack.donaciones.models.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Objeto de valor: la provincia de una dirección postal.
 *
 * MAPEO: @Embeddable anidado dentro de Localidad, que a su vez está dentro de
 * Direccion. Las tres se aplanan en columnas de la tabla que las contiene.
 */
@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Provincia {

    @Column(name = "provincia_nombre")
    private String nombre;
}
