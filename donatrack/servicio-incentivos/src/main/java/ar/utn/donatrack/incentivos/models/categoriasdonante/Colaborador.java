package ar.utn.donatrack.incentivos.models.categoriasdonante;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("COLABORADOR")
public class Colaborador extends CategoriaDonante{
    public Colaborador() {
        super("Colaborador", 1);
    }

    public CategoriaDonante siguienteCategoria(){
        return new Sostenedor();
    }
}
