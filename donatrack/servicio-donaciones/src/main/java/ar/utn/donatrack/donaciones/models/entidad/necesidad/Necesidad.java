package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
 */
@Entity
@Table(name = "necesidad")
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

    @Column(name = "fecha_registro")
    protected LocalDate fechaRegistro;

    @Column(name = "cantidad_objetivo")
    protected int cantidadObjetivo;

    @Column(name = "cantidad_recibida")
    protected int cantidadRecibida;

    public void recibirDonacion(int cantidad) {
        this.cantidadRecibida += cantidad;
    }

    /**
     * Base del algoritmo de compatibilidad semántica: la necesidad sirve para una
     * donación si su nombre o su descripción mencionan la subcategoría donada.
     */
    public boolean esCompatibleCon(String subcategoria) {
        if (subcategoria == null || subcategoria.isBlank()) {
            return false;
        }
        String objetivo = subcategoria.toLowerCase();
        return contieneTexto(nombre, objetivo) || contieneTexto(descripcion, objetivo);
    }

    private boolean contieneTexto(String texto, String objetivo) {
        return texto != null && texto.toLowerCase().contains(objetivo);
    }

    public abstract boolean estaSatisfecha();
}
