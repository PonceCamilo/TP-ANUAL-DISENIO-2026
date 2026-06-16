package ar.utn.donatrack.incentivos.models.Donante.Misiones;

import ar.utn.donatrack.incentivos.models.Donante.Misiones.Mision;
import ar.utn.donatrack.incentivos.dtos.DonanteInfoDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Racha extends Mision{
    private int mesesConsecutivos; // Como los voy actualizando? tengo que ir chequeando mes a mes si se realizo la donacion o no, y actualizar este contador de meses consecutivos. Si un mes no se realizo la donacion, se resetea a 0.
    private int mesesRequeridos; // cantidad de meses consecutivos requeridos para completar la racha. 

    // Consiste en realizar una donacion durante X meses consecutivos. Si un mes no se realiza la donacion, se pierde la racha y se vuelve a empezar. El premio se otorga al alcanzar una racha de X meses consecutivos.
    @Override
    public boolean completada(DonanteInfoDTO donanteInfo) {
        return progresoActual(donanteInfo) >= mesesRequeridos;
    }

    @Override
    public int progresoActual(DonanteInfoDTO donanteInfo) {
        return donanteInfo.getMesesConsecutivosDonando();
    }

    // es6ta logica iria en el servicio de donaciones?
    
    // public void actualizarRacha(DonanteInfoDTO donanteInfo){
    //     if(donanteInfo.realizoDonacionEsteMes()){   // TODO ver si esta codeada y sino hacerla
    //         mesesConsecutivos++; // si se realizo la donacion este mes, incremento el contador de meses consecutivos
    //     } else {
    //         mesesConsecutivos = 0; // si no se realizo la donacion este mes, reseteo el contador de meses consecutivos
    //     }
    // }
}