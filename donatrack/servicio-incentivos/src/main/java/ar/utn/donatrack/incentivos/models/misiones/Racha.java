package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Racha extends Mision {
    private int mesesRequeridos;
    private ProgresoRacha progresoRacha = new ProgresoRacha();

    public Racha(String nombre, String descripcion, CategoriaDonante categoriaRequerida, int mesesRequeridos, Insignia insignia) {
        super(nombre, descripcion, categoriaRequerida, mesesRequeridos, insignia);
        this.mesesRequeridos = mesesRequeridos;
    }

    public boolean estaCompletada(Donante donante) {
        return progresoActual(donante) >= objetivo;
    }

    public int progresoActual(Donante donante) {
        return progresoRacha.mesesConsecutivosDonando(donante, LocalDate.now());
    }

    public boolean perdioProgreso(Donante donante, LocalDate fechaReferencia) {
        return progresoRacha.pasoUnMesCompletoSinDonaciones(donante, fechaReferencia);
    }
}
