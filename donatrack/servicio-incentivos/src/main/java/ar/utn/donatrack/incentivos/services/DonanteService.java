package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.incentivos.interfaces.services.DonanteServiceInterface;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DonanteService implements DonanteServiceInterface {

    private final DonanteRepositoryInterface repositoryInterface;

    // Eliminamos la inyección de DonacionesClient porque las métricas ahora viven en Donante
    public DonanteService(DonanteRepositoryInterface repositoryInterface) {
        this.repositoryInterface = repositoryInterface;
    }

    @Override
    public void procesarAvance(UUID idDonante) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        Mision misionActual = donante.getMisionActual();

        if (misionActual != null && misionActual.estaCompletada(donante)) {
            completarMision(donante, misionActual);
        }
    }

    @Override
    public void completarMision(Donante donante, Mision mision) {
        InsigniaObtenida nuevaInsignia = new InsigniaObtenida(mision.getInsignia(), true);
        donante.addInsignia(nuevaInsignia);

        Mision siguienteMision = donante.getCategoria().siguienteMision(mision);

        if (siguienteMision != null) {
            donante.setMisionActual(siguienteMision);
            donante.setProgresoMisionActual(0);
        } else {
            subirDeCategoria(donante);
        }

        repositoryInterface.save(donante);
    }

    @Override
    public void subirDeCategoria(Donante donante) {
        CategoriaDonante categoriaSiguiente = donante.getCategoria().siguienteCategoria();

        if (categoriaSiguiente != null) {
            donante.setCategoria(categoriaSiguiente);
            donante.setMisionActual(categoriaSiguiente.primeraMision());
            donante.setProgresoMisionActual(0);
        }
    }

    @Override
    public void cambiarVisibilidadInsignia(UUID idDonante, UUID idInsignia, boolean visible) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        donante.getInsigniasObtenidas().stream()
                .filter(insignia -> insignia.getId().equals(idInsignia))
                .findFirst()
                .ifPresent(insignia -> insignia.setVisibilidad(visible));

        repositoryInterface.save(donante);
    }

    @Override
    public int obtenerProgresoMisionActual(UUID idDonante) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        Mision mision = donante.getMisionActual();
        if (mision == null) return 0;

        return mision.progresoActual(donante);
    }

    @Override
    public int obtenerDistanciaRestanteMisionActual(UUID idDonante) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        Mision mision = donante.getMisionActual();
        if (mision == null) return 0;

        return Math.max(0, mision.getObjetivo() - mision.progresoActual(donante));
    }
}