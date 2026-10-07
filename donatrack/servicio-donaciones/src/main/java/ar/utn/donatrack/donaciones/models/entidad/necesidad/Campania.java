package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Agrupador de múltiples Necesidades bajo una misma campaña de la entidad.
 * Permite registrar varias necesidades en un solo paso (ej: tras una inundación).
 *
 * Guarda idEntidad en lugar de la razón social para no quedar inconsistente si
 * la entidad cambia de nombre.
 *
 * MAPEO: la campaña es dueña de sus necesidades (cascade + orphanRemoval): no
 * tienen sentido por fuera de una campaña.
 */
@Entity
@Table(name = "campania")
@NoArgsConstructor
@Getter
@Setter
public class Campania {

    @Id
    @Column(name = "id_campania")
    private UUID idCampania;

    @Column(name = "id_entidad")
    private UUID idEntidad;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "descripcion_general", length = 1000)
    private String descripcionGeneral;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "campania_id")
    private List<Necesidad> necesidades = new ArrayList<>();

    public void agregarNecesidad(Necesidad necesidad) {
        necesidades.add(necesidad);
    }

    public Optional<Necesidad> buscarNecesidad(UUID necesidadId) {
        return necesidades.stream()
                .filter(necesidad -> necesidad.getId().equals(necesidadId))
                .findFirst();
    }

    /** Devuelve true si la necesidad existía y fue eliminada. */
    public boolean eliminarNecesidad(UUID necesidadId) {
        return necesidades.removeIf(necesidad -> necesidad.getId().equals(necesidadId));
    }

    public List<Necesidad> necesidadesCompatiblesCon(String subcategoria) {
        return necesidades.stream()
                .filter(necesidad -> necesidad.esCompatibleCon(subcategoria))
                .toList();
    }
}
