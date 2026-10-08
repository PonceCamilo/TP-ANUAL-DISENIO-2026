package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "racha")
public class Racha extends Mision {
    private int mesesRequeridos;

    @Embedded
    private ProgresoRacha progresoRacha = new ProgresoRacha();

    protected Racha() {
    }

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
