package ar.utn.donatrack.incentivos.models;

import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.models.misiones.ProgresoMision;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Donante {
    private UUID id;
    private CategoriaDonante categoria;
    private List<InsigniaObtenida> insigniasObtenidas = new ArrayList<>();
    private MetricasDonante metricas = new MetricasDonante();
    private ProgresoMision progresoMision = new ProgresoMision();
    private RankingMensual rankingMensual;

    public void addInsignia(InsigniaObtenida insignia) {
        this.insigniasObtenidas.add(insignia);
    }

    public void registrarDonacion(int cantidadBienes, List<String> categoriasDonadas) {
        this.metricas.registrarDonacion(cantidadBienes, categoriasDonadas);
    }

    public void registrarOrganizacionAyudada() {
        this.metricas.registrarDonacionExitosa();
    }

    public Mision misionActual() {
        return progresoMision.getMisionActual();
    }

    public void asignarMisionActual(Mision misionActual) {
        this.progresoMision.setMisionActual(misionActual);
    }

    public int categoriasDistintasDonadas() {
        return metricas.categoriasDistintasDonadas();
    }

    public int totalDonacionesHistoricas() {
        return metricas.totalDonacionesHistoricas();
    }

    public int donacionesDelMesActual() {
        return metricas.donacionesMesActual();
    }

    public int organizacionesAyudadas() {
        return metricas.organizacionesAyudadas();
    }

    public int donacionesExitosas() {
        return metricas.donacionesExitosas();
    }

    public int mayorCantidadBienesEnUnaDonacion() {
        return metricas.recordBienesUnicaDonacion();
    }

    public int mesesConsecutivosDonando() {
        return metricas.mesesConsecutivosDonando(LocalDate.now());
    }

    public boolean perdioRachaPorMesCompletoSinDonar() {
        return metricas.pasoUnMesCompletoSinDonaciones(LocalDate.now());
    }

    public boolean subirCategoria() {
        CategoriaDonante siguiente = this.categoria.siguienteCategoria();
        if (siguiente != null) {
            this.categoria = siguiente;
            return true;
        }
        return false;
    }
}
