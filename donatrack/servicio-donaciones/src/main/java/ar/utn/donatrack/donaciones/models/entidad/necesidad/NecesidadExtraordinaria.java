package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Necesidad que surge ante situaciones excepcionales (inundación, mudanza, etc.).
 * Se satisface cuando la cantidad recibida iguala o supera el objetivo, y una vez
 * cubierta queda cerrada: no vence ni se reinicia.
 */
@Entity
@Table(name = "necesidad_extraordinaria")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor
@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {

    public boolean estaSatisfecha() {
        return this.cantidadRecibida >= this.cantidadObjetivo;
    }
}
