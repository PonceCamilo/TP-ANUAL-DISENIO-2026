package ar.utn.donatrack.donaciones.models.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Objeto de valor que representa una dirección postal.
 * Sin setters: si la dirección cambia, se crea una instancia nueva.
 *
 * MAPEO (Entrega 4): @Embeddable. Se aplana en columnas de la tabla que la
 * contiene, y como la usan tanto PersonaDonante como EntidadBeneficiaria, esas
 * columnas aparecen en las dos tablas. Es lo correcto para un objeto de valor:
 * una dirección no existe por sí sola ni se comparte entre dueños.
 */
@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Direccion {

    @Column(name = "direccion_calle")
    private String calle;

    @Column(name = "direccion_numero")
    private int numero;

    @Embedded
    private Localidad localidad;

    @Column(name = "direccion_codigo_postal")
    private String codigoPostal;
}
