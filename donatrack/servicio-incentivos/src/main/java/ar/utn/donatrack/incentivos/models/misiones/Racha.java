package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
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

    /**
     * Vuelve a crear el calculador de racha cuando Hibernate lo trae en null.
     *
     * ProgresoRacha es un @Embeddable SIN CAMPOS: solo tiene los métodos que
     * cuentan los meses consecutivos. Al no aportar ninguna columna, Hibernate
     * no tiene nada que leer y deja la referencia en null, pisando el
     * `= new ProgresoRacha()` del campo (que solo corre al construir el objeto
     * en memoria, no al cargarlo de la base).
     *
     * Sin esto, toda Racha leída de la base tiraba NullPointerException en
     * progresoActual(), y con ella el job mensual que revisa las rachas.
     * Mismo criterio que Donante.rehidratarProgresoMision().
     */
    @PostLoad
    private void rehidratarProgresoRacha() {
        if (this.progresoRacha == null) {
            this.progresoRacha = new ProgresoRacha();
        }
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
