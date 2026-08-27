package ar.utn.donatrack.incentivos.models.categoriasdonante;

public class Colaborador extends CategoriaDonante{
    public Colaborador() {
        super("Colaborador", 1);
    }

    public CategoriaDonante siguienteCategoria(){
        return new Sostenedor();
    }
}
