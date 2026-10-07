package ar.utn.donatrack.donaciones.models.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Objeto de valor: la localidad de una dirección postal, con su provincia.
 *
 * MAPEO: @Embeddable anidado. Contiene a su vez el @Embeddable Provincia.
 */
@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Localidad {

    @Column(name = "localidad_nombre")
    private String nombre;

    @Embedded
    private Provincia provincia;
}
