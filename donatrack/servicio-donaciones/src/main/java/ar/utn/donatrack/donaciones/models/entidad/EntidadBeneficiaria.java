package ar.utn.donatrack.donaciones.models.entidad;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.EntidadPersistible;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import ar.utn.donatrack.donaciones.models.contacto.Canal;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Organización sin fines de lucro que recibe donaciones: comedores, escuelas
 * rurales, espacios de tutoría.
 *
 * Es dueña de sus campañas, y a través de ellas de sus necesidades, que son el
 * insumo del algoritmo de compatibilidad semántica.
 */
@Entity
@Table(name = "entidad_beneficiaria")
@SuperBuilder
@NoArgsConstructor
@Getter
public class EntidadBeneficiaria extends EntidadPersistible {

    @Column(name = "razon_social")
    private String razonSocial;

    @Embedded
    private Direccion direccion;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "entidad_id")
    private List<MedioDeContacto> contactos = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "entidad_id")
    private List<Representante> representantes = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "entidad_id")
    private List<Campania> campanias = new ArrayList<>();

    /**
     * Reemplaza los contactos modificando la lista existente: es una colección
     * con orphanRemoval y Hibernate no admite que se la cambie por otra. Antes se
     * asignaba una lista nueva (a veces inmutable) y funcionaba solo porque la
     * entidad se guardaba desprendida, con merge.
     */
    public void reemplazarContactos(List<MedioDeContacto> nuevos) {
        contactos.clear();
        contactos.addAll(nuevos);
    }

    /** Igual que reemplazarContactos, para los representantes. */
    public void reemplazarRepresentantes(List<Representante> nuevos) {
        representantes.clear();
        representantes.addAll(nuevos);
    }

    /** Sin setters de clase: cada dato que se puede cambiar tiene su método. */
    public void cambiarRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public void cambiarDireccion(Direccion direccion) {
        this.direccion = direccion;
    }

    /** Listas de solo lectura: se modifican con los métodos de la entidad. */
    public List<MedioDeContacto> getContactos() {
        return contactos == null ? null : Collections.unmodifiableList(contactos);
    }

    public List<Representante> getRepresentantes() {
        return Collections.unmodifiableList(representantes);
    }

    public List<Campania> getCampanias() {
        return Collections.unmodifiableList(campanias);
    }

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
                .filter(contacto -> contacto.canal() == Canal.EMAIL)
                .map(MedioDeContacto::getValor)
                .findFirst()
                .orElse(null);
    }

    /** Contacto por el que se le avisa: su primer email o, si no tiene, su primer contacto. */
    public Optional<MedioDeContacto> contactoParaNotificar() {
        if (contactos == null || contactos.isEmpty()) {
            return Optional.empty();
        }
        return contactos.stream()
                .filter(contacto -> contacto.canal() == Canal.EMAIL)
                .findFirst()
                .or(() -> Optional.of(contactos.getFirst()));
    }

    /**
     * Puntaje del algoritmo de compatibilidad semántica: cuántas necesidades
     * abiertas y compatibles tiene la entidad en sus campañas vigentes a la fecha.
     */
    public int contarNecesidadesCompatiblesCon(Subcategoria subcategoria, LocalDate fecha) {
        return necesidadesAbiertasCompatiblesCon(subcategoria, fecha).size();
    }

    /**
     * Imputa una donación entregada a las necesidades abiertas compatibles con su
     * subcategoría: cada una toma lo que le falta para su objetivo, en orden, y si
     * sobra, el excedente queda en la última, para que lo recibido refleje lo que
     * llegó. Si ninguna necesidad abierta es compatible, no se imputa nada.
     *
     * Las cantidades se toman tal como vienen en los bienes: la necesidad no tiene
     * unidad propia. Devuelve la cantidad imputada.
     */
    public int recibirDonacion(Subcategoria subcategoria, int cantidad, LocalDate fecha) {
        List<Necesidad> abiertas = necesidadesAbiertasCompatiblesCon(subcategoria, fecha);
        if (abiertas.isEmpty() || cantidad <= 0) {
            return 0;
        }
        int restante = cantidad;
        for (Necesidad necesidad : abiertas) {
            int tomado = Math.min(restante, necesidad.faltanteAl(fecha));
            necesidad.recibir(tomado, fecha);
            restante -= tomado;
        }
        if (restante > 0) {
            abiertas.getLast().recibir(restante, fecha);
        }
        return cantidad;
    }

    public int recibirDonacion(Donacion donacion, LocalDate fecha) {
        return recibirDonacion(donacion.getSubcategoria(), donacion.cantidadTotal(), fecha);
    }

    private List<Necesidad> necesidadesAbiertasCompatiblesCon(Subcategoria subcategoria, LocalDate fecha) {
        return campanias.stream()
                .flatMap(campania -> campania.necesidadesAbiertasCompatiblesCon(subcategoria, fecha).stream())
                .toList();
    }
}
