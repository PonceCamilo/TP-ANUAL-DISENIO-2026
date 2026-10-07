package ar.utn.donatrack.logistica.models.referencias;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Referencia local a una donación de servicio-donaciones: solo su id, para que
 * entrega y donacion_lote tengan una foreign key válida. Los datos de la
 * donación siguen viviendo en servicio-donaciones.
 */
@Entity
@Table(name = "donacion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Donacion {
    @Id
    @Column(name = "id_donacion")
    private UUID id;
}
