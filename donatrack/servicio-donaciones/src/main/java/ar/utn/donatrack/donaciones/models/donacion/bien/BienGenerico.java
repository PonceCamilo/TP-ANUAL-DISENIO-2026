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
}
