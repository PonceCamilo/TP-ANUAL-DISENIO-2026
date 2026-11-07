package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persona física habilitada para operar en nombre de una organización.
 *
 * El email es campo directo para poder buscarlo y reemplazarlo sin recorrer la
 * lista de contactos.
 *
 * MAPEO: lo referencian tanto PersonaJuridica como EntidadBeneficiaria, así que
 * la tabla tiene dos FK nullable (persona_juridica_id y entidad_id). Solo una
 * está poblada en cada fila. La alternativa —duplicar la entidad en dos tablas—
 * sería peor: el representante es el mismo concepto en los dos casos.
 */
@Entity
@Table(name = "representante", indexes = {
        @Index(name = "idx_representante_juridica", columnList = "persona_juridica_id"),
        @Index(name = "idx_representante_entidad", columnList = "entidad_id")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Representante {

    @Id
    @Column(name = "id")
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "email")
    private String email;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "representante_id")
    private List<MedioDeContacto> contactos = new ArrayList<>();

    /** A la base llega solo la forma canónica del email, como el de las personas donantes. */
    @PrePersist
    @PreUpdate
    void normalizarEmail() {
        this.email = NormalizadorEmail.normalizar(this.email);
    }
}
