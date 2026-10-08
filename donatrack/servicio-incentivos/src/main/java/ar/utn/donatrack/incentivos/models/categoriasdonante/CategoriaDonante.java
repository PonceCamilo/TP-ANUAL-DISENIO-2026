package ar.utn.donatrack.incentivos.models.categoriasdonante;

import ar.utn.donatrack.incentivos.models.misiones.Mision;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "categoria_donante")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_categoria", discriminatorType = DiscriminatorType.STRING)
public abstract class CategoriaDonante {
    @Id
    private int orden;

    private String nombre;

    @OneToMany(mappedBy = "categoriaRequerida", cascade = CascadeType.ALL)
    private List<Mision> misiones = new ArrayList<>();

    protected CategoriaDonante() {
    }

    protected CategoriaDonante(String nombre, int orden) {
        this.nombre = nombre;
        this.orden = orden;  // se podria sacar esto tal vez
    }

    public Mision primeraMision() {
        if(misiones.isEmpty()) {
            return null;
        }
        return misiones.get(0);
    }

    public Mision siguienteMision(Mision misionActual) {
        int index = misiones.indexOf(misionActual);
        if (index == -1 || index == misiones.size() - 1) {
            return null;
        }
        return misiones.get(index + 1);
    }

    public abstract CategoriaDonante siguienteCategoria();
}
