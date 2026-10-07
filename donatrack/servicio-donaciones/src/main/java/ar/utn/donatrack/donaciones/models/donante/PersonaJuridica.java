package ar.utn.donatrack.donaciones.models.donante;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
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
@Setter
public class PersonaJuridica extends PersonaDonante {

    @Column(name = "razon_social")
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoPersonaJuridica tipo;

    @Column(name = "rubro")
    private String rubro;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_juridica_id")
    private List<Representante> representantes = new ArrayList<>();

    /**
     * El email identifica al representante: volver a agregar a alguien con el
     * mismo email actualiza sus datos en lugar de duplicarlo. La comparación
     * ignora mayúsculas porque los emails no distinguen case en la práctica.
     */
    public void agregarRepresentante(Representante representante) {
        representantes.removeIf(rep -> rep.getEmail() != null
                && rep.getEmail().equalsIgnoreCase(representante.getEmail()));
        representantes.add(representante);
    }
}
