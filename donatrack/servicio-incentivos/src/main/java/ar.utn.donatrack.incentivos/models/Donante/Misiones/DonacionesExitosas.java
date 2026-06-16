package ar.utn.donatrack.incentivos.models.Donante.Misiones;

import ar.utn.donatrack.incentivos.dtos.DonanteInfoDTO;
import ar.utn.donatrack.incentivos.models.Donante.Misiones.Mision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionesExitosas extends Mision{
    // consiste en lograr X donaciones que sean recibidas exitosamente por una entidad beneficiaria. Osea que la entidad tuvo que haber dado el ok
    private int cantidadDonacionesRequerida;
    
    @Override
    public boolean completada(DonanteInfoDTO donanteInfo){    
        return progresoActual(donanteInfo) >= cantidadDonacionesRequerida;
    }

    @Override
    public int progresoActual(DonanteInfoDTO donanteInfo){
        return donanteInfo.getCantidadDonacionesRecibidasExitosamente();
    }
}