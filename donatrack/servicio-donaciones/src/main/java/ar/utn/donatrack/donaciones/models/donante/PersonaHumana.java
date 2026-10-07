package ar.utn.donatrack.donaciones.models.donante;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Persona física donante.
 *
 * Se guarda la fecha de nacimiento y no la edad: la edad se deriva al mapear a
 * DTO, así no queda desactualizada con el paso del tiempo.
 */
@Entity
@Table(name = "persona_humana")
@PrimaryKeyJoinColumn(name = "id")
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public class PersonaHumana extends PersonaDonante {

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /** Se guarda el nombre del enum y no su posición: sobrevive a reordenamientos. */
    @Enumerated(EnumType.STRING)
    @Column(name = "genero")
    private Genero genero;
}
