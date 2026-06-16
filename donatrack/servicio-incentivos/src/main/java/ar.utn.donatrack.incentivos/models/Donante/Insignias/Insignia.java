package ar.utn.donatrack.incentivos.models.Donante.Insignias;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Insignia{
    private UUID id;
    private String nombre;
    private String imagen;
}