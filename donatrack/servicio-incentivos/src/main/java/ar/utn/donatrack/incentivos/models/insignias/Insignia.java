package ar.utn.donatrack.incentivos.models.insignias;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Insignia{
    private UUID id;
    private String nombre;
    private String imagen;
}
