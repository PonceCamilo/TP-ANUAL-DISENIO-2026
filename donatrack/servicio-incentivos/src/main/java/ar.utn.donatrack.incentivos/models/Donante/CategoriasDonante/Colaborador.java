package ar.utn.donatrack.incentivos.models.Donante.CategoriasDonante;

import ar.utn.donatrack.incentivos.models.Donante.CategoriasDonante.CategoriaDonante;

public class Colaborador extends CategoriaDonante{

    public CategoriaDonante siguienteCategoria(){
        // si el colaborador cumple con los requisitos para subir de categoria tiene que poder preguntar cual es la siguiente
        return new Sostenedor();
    }
}