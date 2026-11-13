package ar.utn.donatrack.donaciones.models.donacion.bien;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Bien sin atributos extra: ni vence ni interesa su estado.
 * Todos los genéricos de una misma subcategoría van juntos en una sola donación.
 */
@Entity
@DiscriminatorValue("GENERICO")
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public class BienGenerico extends Bien {

    @Override
    public TipoBien tipo() {
        return TipoBien.GENERICO;
    }

    /** Todos los genéricos de una subcategoría van juntos. */
    @Override
    protected Object criterioDeSegmentacion() {
        return null;
    }

    /** Entrega 6: el mismo bien con otra cantidad, para dividir una donación. */
    @Override
    public Bien copiaCon(int cantidad) {
        return BienGenerico.builder()
                .subcategoria(subcategoria)
                .descripcion(descripcion)
                .foto(foto)
                .cantidad(cantidad)
                .unidad(unidad)
                .build();
    }
}
