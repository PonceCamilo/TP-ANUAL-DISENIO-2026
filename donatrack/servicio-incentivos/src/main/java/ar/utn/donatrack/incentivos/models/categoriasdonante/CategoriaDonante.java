package ar.utn.donatrack.incentivos.models.categoriasdonante;

import ar.utn.donatrack.incentivos.models.misiones.Mision;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class CategoriaDonante {
    private String nombre;
    private int orden;
    private List<Mision> misiones = new ArrayList<>();

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
