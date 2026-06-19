package ar.utn.donatrack.incentivos.models.categoriasdonante;

public class Sostenedor extends CategoriaDonante{

    public CategoriaDonante siguienteCategoria(){
        return new Transformador();
    }
}