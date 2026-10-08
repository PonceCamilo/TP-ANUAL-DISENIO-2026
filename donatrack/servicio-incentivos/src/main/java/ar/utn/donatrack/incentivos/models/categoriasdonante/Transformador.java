package ar.utn.donatrack.incentivos.models.categoriasdonante;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TRANSFORMADOR")
public class Transformador extends CategoriaDonante{
    public Transformador() {
        super("Transformador", 3);
    }

    public CategoriaDonante siguienteCategoria(){
        return null;
    }
}
