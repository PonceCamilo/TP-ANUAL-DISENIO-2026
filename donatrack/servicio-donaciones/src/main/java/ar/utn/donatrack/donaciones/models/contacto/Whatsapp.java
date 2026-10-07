package ar.utn.donatrack.donaciones.models.contacto;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Canal opcional. */
@Entity
@DiscriminatorValue("WHATSAPP")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Whatsapp extends MedioDeContacto {
}
