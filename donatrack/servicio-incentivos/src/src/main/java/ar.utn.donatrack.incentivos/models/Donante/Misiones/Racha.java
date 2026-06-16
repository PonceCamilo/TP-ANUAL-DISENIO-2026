import ar.utn.donatrack.incentivos.models.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class Racha extends Mision{
    int mesesConsecutivos; // Como los voy actualizando? tengo que ir chequeando mes a mes si se realizo la donacion o no, y actualizar este contador de meses consecutivos. Si un mes no se realizo la donacion, se resetea a 0.
    int mesesRequeridos; // cantidad de meses consecutivos requeridos para completar la racha. 

    // Consiste en realizar una donacion durante X meses consecutivos. Si un mes no se realiza la donacion, se pierde la racha y se vuelve a empezar. El premio se otorga al alcanzar una racha de X meses consecutivos.
    public boolean completada(){
        if(mesesConsecutivos >= mesesRequeridos){ 
            return true; 
        }
        return false;
    }

    public void actualizarRacha(Donante donante){   // como obtengo este boolean? tendria que ser un parametro que se le pase a este metodo cada mes, indicando si se realizo la donacion o no ese mes. Este metodo se llamaria cada mes para actualizar el estado de la racha.
        if(donante.realizoDonacionEsteMes()){   // TODO ver si esta codeada y sino hacerla
            mesesConsecutivos++; // si se realizo la donacion este mes, incremento el contador de meses consecutivos
        } else {
            mesesConsecutivos = 0; // si no se realizo la donacion este mes, reseteo el contador de meses consecutivos
        }
    }
}