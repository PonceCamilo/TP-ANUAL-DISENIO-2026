package ar.utn.donatrack.donaciones.models.donacion.bien;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Bien con fecha de vencimiento (alimentos, medicamentos).
 * La segmentación genera donaciones separadas cuando los vencimientos difieren.
 */
@Entity
@DiscriminatorValue("PERECIBLE")
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public class BienPerecible extends Bien {

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
}
