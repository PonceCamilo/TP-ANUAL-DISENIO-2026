package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mision")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Mision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;

    @ManyToOne
    private CategoriaDonante categoriaRequerida; // falto agregar esto al diagrama.

    protected int objetivo;
    private int orden;  // esto se podria sacar.

    @OneToOne(cascade = CascadeType.ALL)
    protected Insignia insignia;

    protected Mision() {
    }

    protected Mision(String nombre, String descripcion, CategoriaDonante categoriaRequerida, int objetivo, Insignia insignia) {
        this(nombre, descripcion, categoriaRequerida, objetivo, 0, insignia);
    }

    protected Mision(String nombre, String descripcion, CategoriaDonante categoriaRequerida, int objetivo, int orden, Insignia insignia) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoriaRequerida = categoriaRequerida;
        this.objetivo = objetivo;
        this.orden = orden;
        this.insignia = insignia;
    }

    public InsigniaObtenida otorgarInsignia() {           // esto falto en el diagrama 
        return new InsigniaObtenida(insignia, true);
    }

    public abstract int progresoActual(Donante donante);

    public int restante(Donante donante) {
        return Math.max(0, this.objetivo - progresoActual(donante));
    }

    public abstract boolean estaCompletada(Donante donante);

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Mision mision)) {
            return false;
        }
        return id != null && id.equals(mision.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
