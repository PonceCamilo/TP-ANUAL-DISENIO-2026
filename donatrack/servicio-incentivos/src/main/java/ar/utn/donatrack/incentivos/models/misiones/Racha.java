package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Racha extends Mision {
    public Racha(String nombre, String descripcion, CategoriaDonante categoriaRequerida,
                 int mesesRequeridos, Insignia insignia) {
        super(nombre, descripcion, categoriaRequerida, mesesRequeridos, insignia);
    }

    @Override
    public boolean estaCompletada(Donante donante) {
        return progresoActual(donante) >= objetivo;
    }

    @Override
    public int progresoActual(Donante donante) {
        return donante.mesesConsecutivosDonando();
    }
}
