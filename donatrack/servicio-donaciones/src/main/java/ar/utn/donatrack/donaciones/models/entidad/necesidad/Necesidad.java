package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Raíz de la jerarquía de necesidades de una EntidadBeneficiaria.
 *
 * MAPEO DE LA HERENCIA (Entrega 4): JOINED.
 *
 * NecesidadRecurrente agrega dos campos con significado propio (la periodicidad
 * y el inicio del período vigente) que NO aplican a una extraordinaria. Con
 * SINGLE_TABLE esas columnas quedarían nullable y nada impediría cargar una
 * extraordinaria con periodicidad, que es un estado imposible en el dominio.
 * JOINED hace que la base misma impida esa inconsistencia.
 *
 * CICLO DE CANTIDADES: lo recibido aumenta cuando se entrega una donación
 * compatible a la entidad (EntidadBeneficiaria.recibirDonacion). Las necesidades
 * satisfechas dejan de sumar puntaje en el matching.
 */
@Entity
@Table(name = "necesidad", indexes = @Index(name = "idx_necesidad_campania", columnList = "campania_id"))
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor
@Getter
@Setter
public abstract class Necesidad {

    @Id
    @Column(name = "id")
    protected UUID id = UUID.randomUUID();

    @Column(name = "nombre")
    protected String nombre;

    @Column(name = "descripcion", length = 1000)
    protected String descripcion;

    /**
     * Lo que la entidad necesita (sillas, ropa de abrigo, arroz…), como pide el
     * enunciado. Es opcional para no romper las necesidades ya cargadas: sin ella,
     * el matching busca la subcategoría en el nombre y la descripción.
     */
    @Embedded
    protected Subcategoria subcategoria;

    @Column(name = "fecha_registro")
    protected LocalDate fechaRegistro;

    @Column(name = "cantidad_objetivo")
    protected int cantidadObjetivo;

    @Column(name = "cantidad_recibida")
    protected int cantidadRecibida;

    public abstract TipoNecesidad tipo();

    /** Datos que se pueden editar (PUT): no incluyen lo recibido ni el período en curso. */
    public void actualizarDatos(String nombre, String descripcion, int cantidadObjetivo, Subcategoria subcategoria) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidadObjetivo = cantidadObjetivo;
        this.subcategoria = subcategoria;
    }

    /** Suma lo recibido en esa fecha (la recurrente, antes, pasa al período que corresponda). */
    public void recibir(int cantidad, LocalDate fecha) {
        this.cantidadRecibida += cantidad;
    }

    /** Lo que falta para el objetivo a esa fecha; 0 si ya se alcanzó. */
    public int faltanteAl(LocalDate fecha) {
        return Math.max(0, cantidadObjetivo - cantidadRecibidaAl(fecha));
    }

    /** Lo recibido que cuenta a esa fecha (en la recurrente, solo lo del período vigente). */
    public int cantidadRecibidaAl(LocalDate fecha) {
        return cantidadRecibida;
    }

    public boolean estaSatisfechaAl(LocalDate fecha) {
        return cantidadRecibidaAl(fecha) >= cantidadObjetivo;
    }

    /**
     * Si una donación de esa subcategoría sirve para esta necesidad. Si la
     * necesidad declara su subcategoría, se comparan las dos normalizadas; si no,
     * se busca la subcategoría en el nombre o la descripción, con la misma
     * normalización (sin acentos, mayúsculas ni espacios de más).
     */
    public boolean esCompatibleCon(Subcategoria buscada) {
        if (buscada == null || buscada.estaVacia()) {
            return false;
        }
        if (subcategoria != null && !subcategoria.estaVacia()) {
            return subcategoria.coincideCon(buscada);
        }
        return buscada.estaMencionadaEn(nombre) || buscada.estaMencionadaEn(descripcion);
    }
}
