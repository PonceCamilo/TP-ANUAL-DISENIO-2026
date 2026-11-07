package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.MetricasDonante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "donaciones_exitosas")
public class DonacionesExitosas extends Mision {
    private int cantidadDonacionesRequerida;

    protected DonacionesExitosas() {
    }

    public DonacionesExitosas(String nombre, String descripcion, CategoriaDonante categoriaRequerida, int objetivo, Insignia insignia) {
        super(nombre, descripcion, categoriaRequerida, objetivo, insignia);
        this.cantidadDonacionesRequerida = objetivo;
    }

    public boolean estaCompletada(Donante donante) {    
        return progresoActual(donante) >= objetivo;
    }

    public int progresoActual(Donante donante) {
        return new MetricasDonante().donacionesExitosas(donante);
    }
}
