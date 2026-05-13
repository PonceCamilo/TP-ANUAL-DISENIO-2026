package ar.utn.donatrack.donaciones.model.categoria;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "tipo") // genera automaticamente equals() y hashCode()
public class Subcategoria {

    private int idCategoria;
    private String tipo;
    private String nombre;

    public Subcategoria(String tipo, String nombre) {
        this.tipo = tipo;
        this.nombre = nombre;
    }
}
