package ar.utn.donatrack.incentivos.models.Donante.Misiones;

import ar.utn.donatrack.incentivos.dtos.DonanteInfoDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HabilDonador extends Mision{
    // consiste en tener una donacion que supere X cantidad de bienes
    private int cantidadBienesRequerida;

    @Override
    public boolean completada(DonanteInfoDTO donanteInfo){     
        return progresoActual(donanteInfo) >= cantidadBienesRequerida;
    }

    @Override
    public int progresoActual(DonanteInfoDTO donanteInfo){     // a que considero progresoActual en habilDonador? --> Para mi es el record de bienes donados en una unica donacion.
        return donanteInfo.getCantidadRecordBienesEnUnicaDonacion(); // TODO ver si existe este metodo y sino programarlo
    }
}