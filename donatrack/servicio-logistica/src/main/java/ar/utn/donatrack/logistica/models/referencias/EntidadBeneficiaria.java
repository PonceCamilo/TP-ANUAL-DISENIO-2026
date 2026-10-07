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
 * Referencia local a una entidad beneficiaria de servicio-donaciones: solo su
 * id, para que parada, entrega y donacion_lote tengan una foreign key válida.
 */
@Entity
@Table(name = "entidad_beneficiaria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EntidadBeneficiaria {
    @Id
    @Column(name = "id_entidad_beneficiaria")
    private UUID id;
}
