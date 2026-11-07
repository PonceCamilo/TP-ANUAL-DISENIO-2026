package ar.utn.donatrack.incentivos.models.categoriasdonante;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SOSTENEDOR")
public class Sostenedor extends CategoriaDonante{
    public Sostenedor() {
        super("Sostenedor", 2);
    }

    public CategoriaDonante siguienteCategoria(){
        return new Transformador();
    }
}
