package ar.utn.donatrack.donaciones.models.entidad;

import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Organización sin fines de lucro que recibe donaciones: comedores, escuelas
 * rurales, espacios de tutoría.
 *
 * Es dueña de sus campañas, y a través de ellas de sus necesidades, que son el
 * insumo del algoritmo de compatibilidad semántica.
 */
@Entity
@Table(name = "entidad_beneficiaria")
@Builder
@NoArgsConstructor
@lombok.AllArgsConstructor
@Getter
@Setter
public class EntidadBeneficiaria {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "razon_social")
    private String razonSocial;

    @Embedded
    private Direccion direccion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "entidad_id")
    private List<MedioDeContacto> contactos;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "entidad_id")
    private List<Representante> representantes;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "entidad_id")
    private List<Campania> campanias = new ArrayList<>();

    public void agregarCampania(Campania campania) {
        campanias.add(campania);
    }

    public void agregarNecesidadACampania(Campania campania, Necesidad necesidad) {
        this.getCampanias().stream()
                .filter(c -> c.getIdCampania().equals(campania.getIdCampania()))
                .findFirst()
                .ifPresent(c -> c.agregarNecesidad(necesidad));
    }

    /** Canal por el que el sistema le avisa de asignaciones y entregas. */
    public String obtenerEmail() {
        if (contactos == null) {
            return null;
        }
        return contactos.stream()
                .filter(c -> c instanceof Email)
                .map(MedioDeContacto::getValor)
                .findFirst()
                .orElse(null);
    }

    /** Puntaje del algoritmo de compatibilidad semántica: a más coincidencias, más prioridad. */
    public int contarNecesidadesCompatiblesCon(String subcategoria) {
        return campanias.stream()
                .mapToInt(campania -> campania.necesidadesCompatiblesCon(subcategoria).size())
                .sum();
    }
}
