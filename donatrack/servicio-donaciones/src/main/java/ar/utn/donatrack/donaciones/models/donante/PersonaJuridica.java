package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Organización donante (gubernamental, ONG, empresa o institución).
 * Opera a través de representantes habilitados.
 */
@Entity
@Table(name = "persona_juridica")
@PrimaryKeyJoinColumn(name = "id")
@SuperBuilder
@NoArgsConstructor
@Getter
public class PersonaJuridica extends PersonaDonante {

    @Column(name = "razon_social")
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoPersonaJuridica tipo;

    @Column(name = "rubro")
    private String rubro;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_juridica_id")
    private List<Representante> representantes = new ArrayList<>();

    /**
     * El email identifica al representante: volver a agregar a alguien con el
     * mismo email actualiza sus datos en lugar de duplicarlo. La comparación
     * ignora mayúsculas porque los emails no distinguen case en la práctica.
     */
    @Override
    public TipoPersona tipoPersona() {
        return TipoPersona.JURIDICA;
    }

    @Override
    public void actualizarNombreImportado(String razonSocial) {
        this.razonSocial = razonSocial.trim();
    }

    /**
     * Un representante con el mismo email reemplaza al anterior. Se compara por la
     * forma canónica del email (NormalizadorEmail), como en el resto del servicio:
     * antes era equalsIgnoreCase y un espacio de más duplicaba al representante.
     */
    public void agregarRepresentante(Representante representante) {
        String email = NormalizadorEmail.normalizar(representante.getEmail());
        representantes.removeIf(rep -> rep.getEmail() != null && NormalizadorEmail.normalizar(rep.getEmail()).equals(email));
        representantes.add(representante);
    }

    /** Los representantes, de solo lectura: se agregan con agregarRepresentante. */
    public List<Representante> getRepresentantes() {
        return Collections.unmodifiableList(representantes);
    }

    /** Los datos que se pueden editar; los que vienen en null no se modifican. */
    public void actualizarDatosDeOrganizacion(String razonSocial, String rubro) {
        if (razonSocial != null) this.razonSocial = razonSocial;
        if (rubro != null) this.rubro = rubro;
    }
}
