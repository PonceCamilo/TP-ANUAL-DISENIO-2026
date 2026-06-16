import ar.utn.donatrack.incentivos.models.CategoriasDonante.CategoriaDonante;

abstract class Colaborador extends CategoriaDonante{

    public CategoriaDonante siguienteCategoria(){
        // si el colaborador cumple con los requisitos para subir de categoria tiene que poder preguntar cual es la siguiente
        return Sostenedor.getInstance();
    }
}