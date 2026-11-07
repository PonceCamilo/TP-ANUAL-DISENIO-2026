package ar.utn.donatrack.donaciones.models.contacto;

import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Canal de contacto por correo electrónico.
 *
 * La identidad de una persona donante es su campo `email`, no este contacto:
 * un contacto EMAIL es un canal más (por ejemplo, el medio predeterminado o un
 * correo alternativo). Se guarda en la misma forma canónica que la identidad
 * para que dos valores que solo difieren en mayúsculas no se vean distintos.
 */
@Entity
@DiscriminatorValue("EMAIL")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Email extends MedioDeContacto {

    @PrePersist
    @PreUpdate
    private void normalizarValor() {
        this.valor = NormalizadorEmail.normalizar(this.valor);
    }

    @Override
    public Canal canal() {
        return Canal.EMAIL;
    }
}
