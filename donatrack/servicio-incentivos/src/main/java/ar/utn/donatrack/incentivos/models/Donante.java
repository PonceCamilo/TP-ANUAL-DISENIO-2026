package ar.utn.donatrack.incentivos.models;

import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
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
    private CategoriaDonante categoriaActual;
    private List<InsigniaObtenida> insigniasObtenidas = new ArrayList<>();
    private List<DonacionRegistrada> donaciones = new ArrayList<>();
    private ProgresoMision progresoMision = new ProgresoMision();

    public CategoriaDonante getCategoria() {
        return categoriaActual;
    }

    public void setCategoria(CategoriaDonante categoria) {
        this.categoriaActual = categoria;
    }

    public void registrarDonacion(DonacionRegistrada donacion) {
        this.donaciones.add(donacion);
    }

    public void completarMision(Mision mision) {
        agregarInsignia(mision.otorgarInsignia());
    }

    public boolean subirCategoria() {
        CategoriaDonante siguiente = this.categoriaActual.siguienteCategoria();
        if (siguiente != null) {
            this.categoriaActual = siguiente;
            cambiarMisionActual(siguiente.primeraMision());
            return true;
        }
        return false;
    }

    public void agregarInsignia(InsigniaObtenida insignia) {
        this.insigniasObtenidas.add(insignia);
    }

    public void cambiarVisibilidadInsignia(UUID idInsignia, boolean visible) {
        insigniasObtenidas.stream()
                .filter(insignia -> insignia.getId().equals(idInsignia))
                .findFirst()
                .ifPresent(insignia -> insignia.setVisibilidad(visible));
    }

    public void cambiarMisionActual(Mision mision) {
        this.progresoMision.cambiarMisionActual(mision);
    }

    public int misionesCompletadasEnPeriodo(int mes, int anio) {
        return (int) insigniasObtenidas.stream()
                .filter(insignia -> insignia.getFechaObtencion() != null)
                .filter(insignia -> insignia.getFechaObtencion().getMonthValue() == mes)
                .filter(insignia -> insignia.getFechaObtencion().getYear() == anio)
                .count();
    }
}
