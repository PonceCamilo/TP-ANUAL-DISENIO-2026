package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
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
 * MAPEO (Entrega 5): idEntidad se lee de la columna entidad_id, la misma FK que
 * escribe EntidadBeneficiaria al guardar sus campañas. Hasta la Entrega 4 había
 * además una columna id_entidad que repetía el mismo dato: era una redundancia
 * sin beneficio de lectura y podía quedar distinta de la FK.
 *
 * MAPEO: la campaña es dueña de sus necesidades (cascade + orphanRemoval): no
 * tienen sentido por fuera de una campaña.
 */
@Entity
@Table(name = "campania", indexes = @Index(name = "idx_campania_entidad", columnList = "entidad_id"))
@NoArgsConstructor
@Getter
@Setter
public class Campania {

    @Id
    @Column(name = "id_campania")
    private UUID idCampania;

    @Column(name = "entidad_id", insertable = false, updatable = false)
    private UUID idEntidad;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "descripcion_general", length = 1000)
    private String descripcionGeneral;

    @BatchSize(size = 100)
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

    /** Una campaña sin fecha de inicio o de fin no está acotada de ese lado. */
    public boolean estaVigenteAl(LocalDate fecha) {
        return (fechaInicio == null || !fecha.isBefore(fechaInicio))
                && (fechaFin == null || !fecha.isAfter(fechaFin));
    }

    /**
     * Necesidades que todavía pueden recibir una donación de esa subcategoría a
     * la fecha: compatibles y sin satisfacer, en una campaña vigente. Antes se
     * contaban también las satisfechas y las de campañas terminadas.
     */
    public List<Necesidad> necesidadesAbiertasCompatiblesCon(Subcategoria subcategoria, LocalDate fecha) {
        if (!estaVigenteAl(fecha)) {
            return List.of();
        }
        return necesidades.stream()
                .filter(necesidad -> necesidad.esCompatibleCon(subcategoria))
                .filter(necesidad -> !necesidad.estaSatisfechaAl(fecha))
                .toList();
    }
}
