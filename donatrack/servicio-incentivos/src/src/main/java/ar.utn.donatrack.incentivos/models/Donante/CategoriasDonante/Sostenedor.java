import ar.utn.donatrack.incentivos.models.CategoriasDonante.CategoriaDonante;

abstract class Sostenedor extends CategoriaDonante{

    public CategoriaDonante siguienteCategoria(){
        return Transformador.getInstance();
    }
}