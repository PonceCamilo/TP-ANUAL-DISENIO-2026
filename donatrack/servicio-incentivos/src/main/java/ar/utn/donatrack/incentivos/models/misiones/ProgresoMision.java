package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.DonacionRegistrada;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Embeddable
public class ProgresoMision {
    @Getter
    @ManyToOne
    private Mision misionActual;

    // Los perfiles anteriores al cambio pueden tener NULL hasta registrar actividad.
    private Integer cantidadProgresoMision;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "progreso_mision_categoria", joinColumns = @JoinColumn(name = "donante_id"))
    @Column(name = "categoria")
    private Set<String> categoriasContadas = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "progreso_mision_mes", joinColumns = @JoinColumn(name = "donante_id"))
    @Column(name = "mes")
    private Set<LocalDate> mesesContados = new HashSet<>();

    public void registrarActividad(DonacionRegistrada donacion) {
        if (misionActual != null) {
            misionActual.registrarActividad(this, donacion);
        }
    }

    public boolean completada(LocalDate fechaReferencia) {
        return misionActual != null && progresoActual(fechaReferencia) >= misionActual.getObjetivo();
    }

    public int progresoActual(LocalDate fechaReferencia) {
        if (misionActual instanceof Racha) {
            return new ProgresoRacha().mesesConsecutivosDonando(mesesDeRacha(), fechaReferencia);
        }
        return progresoNumerico();
    }

    public void reiniciarRachaVencida(LocalDate fechaReferencia) {
        if (misionActual instanceof Racha
                && new ProgresoRacha().pasoUnMesCompletoSinDonaciones(mesesDeRacha(), fechaReferencia)) {
            mesesContados.clear();
        }
    }

    void registrarMaximoDeBienes(int bienes) {
        cantidadProgresoMision = Math.max(progresoNumerico(), bienes);
    }

    void contarEntregaExitosa() {
        cantidadProgresoMision = progresoNumerico() + 1;
    }

    void registrarCategorias(Set<String> categorias) {
        categoriasContadas.addAll(categorias);
        cantidadProgresoMision = categoriasContadas.size();
    }

    void registrarMes(YearMonth mes) {
        mesesContados.add(mes.atDay(1));
    }

    private int progresoNumerico() {
        return cantidadProgresoMision == null ? 0 : cantidadProgresoMision;
    }

    private Set<YearMonth> mesesDeRacha() {
        return mesesContados.stream().map(YearMonth::from).collect(Collectors.toSet());
    }

    public void cambiarMisionActual(Mision mision) {
        this.misionActual = mision;
        cantidadProgresoMision = 0;
        categoriasContadas.clear();
        mesesContados.clear();
    }
}
