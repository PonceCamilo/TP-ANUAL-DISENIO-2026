package ar.utn.donatrack.incentivos.models.Donante.CategoriasDonante;

import ar.utn.donatrack.incentivos.models.Donante.CategoriasDonante.CategoriaDonante;

public class Sostenedor extends CategoriaDonante{

    public CategoriaDonante siguienteCategoria(){
        return new Transformador();
    }
}