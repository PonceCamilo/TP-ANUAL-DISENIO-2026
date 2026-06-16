import ar.utn.donatrack.incentivos.models.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class HabilDonador extends Mision{
    // consiste en tener una donacion que supere X cantidad de bienes
    int cantidadBienesRequerida;

    public boolean completada(Donante donante){     
        return progresoActual(donante) >= cantidadBienesRequerida;
    }

    public int progresoActual(Donante donante){     // a que considero progresoActual en habilDonador? --> Para mi es el record de bienes donados en una unica donacion.
        return donante.cantidadRecordBienesEnUnicaDonacion(); // TODO ver si existe este metodo y sino programarlo
    }
}