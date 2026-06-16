import ar.utn.donatrack.incentivos.models.Insignias.Insignia;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class InsigniaObtenida {
    private Insignia insignia;
    private boolean visibilidad;
    private LocalDate fechaObtencion; // podria ser un LocalDate o algo asi, pero por simplicidad lo dejo como String


}