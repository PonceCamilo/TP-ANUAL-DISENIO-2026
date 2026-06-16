package ar.utn.donatrack.incentivos.models.Donante.Misiones;

import ar.utn.donatrack.incentivos.dtos.DonanteInfoDTO;
import ar.utn.donatrack.incentivos.models.Donante.Insignias.Insignia;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Mision {
    private UUID id;
    protected int objetivo; 
    protected Insignia insignia;
    
    // recibe el DTO no al donante local
    public abstract boolean completada(DonanteInfoDTO donanteInfo);
    public abstract int progresoActual(DonanteInfoDTO donanteInfo); 
}