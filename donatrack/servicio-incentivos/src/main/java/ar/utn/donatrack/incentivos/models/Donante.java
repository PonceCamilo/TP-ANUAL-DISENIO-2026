package ar.utn.donatrack.incentivos.models;

import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Donante {
    private UUID id;
    private CategoriaDonante categoria;
    private Mision misionActual;
    private int progresoMisionActual;
    private List<InsigniaObtenida> insigniasObtenidas = new ArrayList<>();

    // Métricas generales
    private int totalDonacionesHistoricas;
    private int donacionesMesActual;
    private int organizacionesAyudadas;
    private int posicionRanking;

    // Métricas específicas para Misiones
    private int categoriasDistintasDonadas;
    private int donacionesExitosas;
    private int recordBienesUnicaDonacion;
    private int mesesConsecutivosDonando;

    public void addInsignia(InsigniaObtenida insignia) {
        this.insigniasObtenidas.add(insignia);
    }

    public void registrarDonacion(int cantidadBienes) {
        this.totalDonacionesHistoricas++;
        this.donacionesMesActual++;
        if (cantidadBienes > this.recordBienesUnicaDonacion) {
            this.recordBienesUnicaDonacion = cantidadBienes;
        }
    }

    public void registrarOrganizacionAyudada() {
        this.organizacionesAyudadas++;
        this.donacionesExitosas++;
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