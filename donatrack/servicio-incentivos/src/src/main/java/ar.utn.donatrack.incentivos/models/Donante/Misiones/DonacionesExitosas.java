import ar.utn.donatrack.incentivos.models.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class DonacionesExitosas extends Mision{
    // consiste en lograr X donaciones que sean recibidas exitosamente por una entidad beneficiaria. Osea que la entidad tuvo que haber dado el ok
    int cantidadDonacionesRequerida;
    
    public boolean completada(Donante donante){    
        return progresoActual(donante) >= cantidadDonacionesRequerida;
    }

    public int progresoActual(Donante donante){
        return donante.donacionesRecibidasExitosamente();
    }
}