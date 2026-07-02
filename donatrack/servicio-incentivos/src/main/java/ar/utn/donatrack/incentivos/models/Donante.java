package ar.utn.donatrack.incentivos.models;

import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.ProgresoMision;
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

    public boolean subirCategoria() {
        CategoriaDonante siguiente = this.categoria.siguienteCategoria();
        if (siguiente != null) {
            this.categoria = siguiente;
            return true;
        }
        return false;
    }
}
