package ar.utn.donatrack.donaciones.models.categoria;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "tipo") // genera automaticamente equals() y hashCode()
public class Subcategoria {

    private String tipo;

    public Subcategoria(String tipo) {
        this.tipo = tipo;
    }
}
