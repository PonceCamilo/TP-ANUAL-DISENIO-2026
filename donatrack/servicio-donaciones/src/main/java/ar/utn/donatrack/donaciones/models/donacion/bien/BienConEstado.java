package ar.utn.donatrack.donaciones.models.donacion.bien;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Bien en el que importa si es nuevo o usado (mobiliario, vestimenta).
 * La segmentación separa los nuevos de los usados dentro de la misma subcategoría.
 */
@Entity
@DiscriminatorValue("CON_ESTADO")
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public class BienConEstado extends Bien {

    @Column(name = "es_nuevo")
    private boolean esNuevo;
}
