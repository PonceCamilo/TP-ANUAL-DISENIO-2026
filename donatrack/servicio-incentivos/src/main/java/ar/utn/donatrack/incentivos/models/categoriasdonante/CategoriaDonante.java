package ar.utn.donatrack.incentivos.models.categoriasdonante;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CategoriaDonante{
    private String nombre;

    public abstract CategoriaDonante siguienteCategoria();
}
