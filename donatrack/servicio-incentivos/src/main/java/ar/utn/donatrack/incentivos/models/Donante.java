package ar.utn.donatrack.incentivos.models;

import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.*;

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
    private Set<String> categoriasHistorial = new HashSet<>();
    private int donacionesExitosas;
    private int recordBienesUnicaDonacion;
    private int mesesConsecutivosDonando;

    // Evolución por período (Key: "YYYY-MM", Value: cantidad)
    private Map<String, Integer> evolucionPeriodo = new HashMap<>();

    public void addInsignia(InsigniaObtenida insignia) {
        this.insigniasObtenidas.add(insignia);
    }

    // ACÁ ESTÁ LA CORRECCIÓN: Ahora recibe la cantidad y la lista de categorías
    public void registrarDonacion(int cantidadBienes, List<String> categoriasDonadas) {
        this.totalDonacionesHistoricas++;
        this.donacionesMesActual++;

        if (cantidadBienes > this.recordBienesUnicaDonacion) {
            this.recordBienesUnicaDonacion = cantidadBienes;
        }

        // Registrar categorías únicas para la misión Completitud
        if (categoriasDonadas != null) {
            this.categoriasHistorial.addAll(categoriasDonadas);
        }

        // Registrar evolución mensual
        String periodoActual = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
        this.evolucionPeriodo.put(periodoActual, this.evolucionPeriodo.getOrDefault(periodoActual, 0) + 1);
    }

    public int getCategoriasDistintasDonadas() {
        return this.categoriasHistorial.size();
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