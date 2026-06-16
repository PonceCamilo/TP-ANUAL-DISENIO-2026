package ar.utn.donatrack.incentivos.models.Donante.Insignias;

import ar.utn.donatrack.incentivos.models.Donante.Insignias.Insignia;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class InsigniaObtenida {
    private UUID id;
    private Insignia insignia;

    private boolean visibilidad;
    private LocalDate fechaObtencion; // no lo pide pero me parece que es importante tenerlo

    public InsigniaObtenida(Insignia insignia, boolean visibilidad) {
        this.id = UUID.randomUUID();
        this.insignia = insignia;
        this.visibilidad = visibilidad;
        this.fechaObtencion = LocalDate.now();
    }
    
}