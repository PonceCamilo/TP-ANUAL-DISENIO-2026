package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.incentivos.interfaces.services.DonanteServiceInterface;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.repositories.IncentivosRepositorioEnMemoria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DonanteService implements DonanteServiceInterface {

    private final DonanteRepositoryInterface repositoryInterface;
    private final IncentivosRepositorioEnMemoria incentivosRepositorio;

    public DonanteService(DonanteRepositoryInterface repositoryInterface,
                          IncentivosRepositorioEnMemoria incentivosRepositorio) {
        this.repositoryInterface = repositoryInterface;
        this.incentivosRepositorio = incentivosRepositorio;
    }

    @Override
    public void procesarAvance(UUID idDonante) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        Mision misionActual = donante.misionActual();
        if (misionActual != null && misionActual.estaCompletada(donante)) {
            completarMision(donante, misionActual);
        }
    }

    @Override
    public void completarMision(Donante donante, Mision mision) {
        InsigniaObtenida nuevaInsignia = new InsigniaObtenida(mision.getInsignia(), true);
        donante.addInsignia(nuevaInsignia);

        Mision siguienteMision = siguienteMisionDeCategoria(donante, mision);
        if (siguienteMision != null) {
            donante.asignarMisionActual(siguienteMision);
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
            donante.asignarMisionActual(primeraMisionDeCategoria(categoriaSiguiente));
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

        Mision mision = donante.misionActual();
        if (mision == null) return 0;

        return mision.progresoActual(donante);
    }

    @Override
    public int obtenerDistanciaRestanteMisionActual(UUID idDonante) {
        Donante donante = repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new RuntimeException("Donante no encontrado"));

        Mision mision = donante.misionActual();
        if (mision == null) return 0;

        return mision.restante(donante);
    }

    private Mision primeraMisionDeCategoria(CategoriaDonante categoria) {
        List<Mision> misiones = incentivosRepositorio.listarMisionesPorCategoria(categoria);
        return misiones.isEmpty() ? null : misiones.get(0);
    }

    private Mision siguienteMisionDeCategoria(Donante donante, Mision misionActual) {
        List<Mision> misiones = incentivosRepositorio.listarMisionesPorCategoria(donante.getCategoria());
        int index = misiones.indexOf(misionActual);
        if (index == -1 || index == misiones.size() - 1) {
            return null;
        }
        return misiones.get(index + 1);
    }
}
