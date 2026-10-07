package ar.utn.donatrack.donaciones.models.contacto;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Canal opcional. El servicio de notificaciones lo usa para enviar SMS. */
@Entity
@DiscriminatorValue("TELEFONO")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Telefono extends MedioDeContacto {
}
