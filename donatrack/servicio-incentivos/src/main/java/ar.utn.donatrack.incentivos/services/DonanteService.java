package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.clients.DonacionesClient;
import ar.utn.donatrack.incentivos.dtos.DonanteInfoDTO;
import ar.utn.donatrack.incentivos.models.Donante.Donante;
import ar.utn.donatrack.incentivos.models.Donante.CategoriasDonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.Donante.Insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.Donante.Misiones.Mision;
import ar.utn.donatrack.incentivos.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.incentivos.interfaces.services.DonanteServiceInterface;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DonanteService implements DonanteServiceInterface {

    private final DonanteRepositoryInterface repositoryInterface;
    private final DonacionesClient donacionesClient;

    public DonanteService(DonanteRepositoryInterface repositoryInterface, DonacionesClient donacionesClient) {
        this.repositoryInterface = repositoryInterface;
        this.donacionesClient = donacionesClient;
    }

    @Override
    public void procesarAvance(UUID idDonante) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        DonanteInfoDTO donanteInfo = donacionesClient.obtenerInfoDonante(idDonante);
        Mision misionActual = donante.getMisionActual();

        if (misionActual != null && misionActual.completada(donanteInfo)) {
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
                .ifPresent(insignia -> insignia.setVisibilidad(visible)); // setVisibilidad, no setVisible

        repositoryInterface.save(donante);
    }

    @Override
    public int obtenerProgresoMisionActual(UUID idDonante) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        Mision mision = donante.getMisionActual();
        if (mision == null) return 0; // Guardaplast por si el donante no tiene misión activa

        DonanteInfoDTO donanteInfo = donacionesClient.obtenerInfoDonante(idDonante);
        return mision.progresoActual(donanteInfo);
    }

    @Override
    public int obtenerDistanciaRestanteMisionActual(UUID idDonante) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        Mision mision = donante.getMisionActual();
        if (mision == null) return 0;

        DonanteInfoDTO donanteInfo = donacionesClient.obtenerInfoDonante(idDonante);
        
        // Math.max evita que si el donante se recontra pasó del objetivo, devuelva números negativos
        return Math.max(0, mision.getObjetivo() - mision.progresoActual(donanteInfo)); 
    }
}
