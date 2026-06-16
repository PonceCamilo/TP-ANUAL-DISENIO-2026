import ar.utn.donatrack.incentivos.models.CategoriasDonante.CategoriaDonante;

abstract class Transformador extends CategoriaDonante{

    public CategoriaDonante siguienteCategoria(){
        return null;  // TODO hacer que tire una excepcion, no hay siguiente categoria
    }
}