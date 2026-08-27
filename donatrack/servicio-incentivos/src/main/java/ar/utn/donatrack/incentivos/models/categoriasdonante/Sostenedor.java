package ar.utn.donatrack.incentivos.models.categoriasdonante;

public class Sostenedor extends CategoriaDonante{
    public Sostenedor() {
        super("Sostenedor", 2);
    }

    public CategoriaDonante siguienteCategoria(){
        return new Transformador();
    }
}
