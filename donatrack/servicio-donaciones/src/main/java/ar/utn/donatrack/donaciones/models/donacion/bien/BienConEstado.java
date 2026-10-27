package ar.utn.donatrack.donaciones.models.donacion.bien;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

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

    @Override
    public TipoBien tipo() {
        return TipoBien.CON_ESTADO;
    }

    @Override
    public Optional<Boolean> nuevo() {
        return Optional.of(esNuevo);
    }

    /** Los nuevos y los usados van en donaciones separadas. */
    @Override
    protected Object criterioDeSegmentacion() {
        return esNuevo;
    }
}
