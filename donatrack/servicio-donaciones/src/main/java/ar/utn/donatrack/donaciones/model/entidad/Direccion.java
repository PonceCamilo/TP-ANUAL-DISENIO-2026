package ar.utn.donatrack.donaciones.model.entidad;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Direccion {
    private String calle;
    private int altura;
    private String ciudad;
    private String provincia;
    private String pais;
    private String codigoPostal;
}