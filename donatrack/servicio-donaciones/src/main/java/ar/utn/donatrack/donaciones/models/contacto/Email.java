package ar.utn.donatrack.donaciones.models.contacto;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Canal obligatorio de toda persona donante: es su clave de identidad y de contacto. */
@Entity
@DiscriminatorValue("EMAIL")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Email extends MedioDeContacto {
}
