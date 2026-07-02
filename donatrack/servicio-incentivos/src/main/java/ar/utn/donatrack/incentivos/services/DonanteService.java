package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.exceptions.DonanteNoEncontradoException;
import ar.utn.donatrack.incentivos.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.incentivos.interfaces.services.DonanteServiceInterface;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.repositories.IncentivosRepositorioEnMemoria;
import ar.utn.donatrack.incentivos.validations.IncentivosValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DonanteService implements DonanteServiceInterface {

    private final DonanteRepositoryInterface repositoryInterface;
    private final IncentivosRepositorioEnMemoria incentivosRepositorio;
    private final IncentivosValidator validator;

    public DonanteService(DonanteRepositoryInterface repositoryInterface,
                          IncentivosRepositorioEnMemoria incentivosRepositorio,
                          IncentivosValidator validator) {
        this.repositoryInterface = repositoryInterface;
        this.incentivosRepositorio = incentivosRepositorio;
        this.validator = validator;
    }

    public void procesarAvance(UUID idDonante) {
        Donante donante = obtenerDonante(idDonante);

        Mision misionActual = donante.getProgresoMision().getMisionActual();
        if (misionActual != null && misionActual.estaCompletada(donante)) {
            completarMision(donante, misionActual);
        }
    }

    public void completarMision(Donante donante, Mision mision) {
        InsigniaObtenida nuevaInsignia = new InsigniaObtenida(mision.getInsignia(), true);
        donante.addInsignia(nuevaInsignia);

        Mision siguienteMision = siguienteMisionDeCategoria(donante, mision);
        if (siguienteMision != null) {
            donante.getProgresoMision().setMisionActual(siguienteMision);
        } else {
            subirDeCategoria(donante);
        }

        repositoryInterface.save(donante);
    }

    public void subirDeCategoria(Donante donante) {
        CategoriaDonante categoriaSiguiente = donante.getCategoria().siguienteCategoria();

        if (categoriaSiguiente != null) {
            donante.setCategoria(categoriaSiguiente);
            donante.getProgresoMision().setMisionActual(primeraMisionDeCategoria(categoriaSiguiente));
        }
    }

    public void cambiarVisibilidadInsignia(UUID idDonante, UUID idInsignia, boolean visible) {
        Donante donante = obtenerDonante(idDonante);

        donante.getInsigniasObtenidas().stream()
                .filter(insignia -> insignia.getId().equals(idInsignia))
                .findFirst()
                .ifPresent(insignia -> insignia.setVisibilidad(visible));

        repositoryInterface.save(donante);
    }

    public int obtenerProgresoMisionActual(UUID idDonante) {
        Donante donante = obtenerDonante(idDonante);

        Mision mision = donante.getProgresoMision().getMisionActual();
        if (mision == null) return 0;

        return mision.progresoActual(donante);
    }

    public int obtenerDistanciaRestanteMisionActual(UUID idDonante) {
        Donante donante = obtenerDonante(idDonante);

        Mision mision = donante.getProgresoMision().getMisionActual();
        if (mision == null) return 0;

        return mision.restante(donante);
    }

    private Donante obtenerDonante(UUID idDonante) {
        return repositoryInterface.findById(idDonante)
                .orElseThrow(() -> new DonanteNoEncontradoException(idDonante));
    }

    private Mision primeraMisionDeCategoria(CategoriaDonante categoria) {
        List<Mision> misiones = incentivosRepositorio.listarMisionesPorCategoria(categoria);
        validator.validarMisionesDisponibles(misiones, categoria);
        return misiones.get(0);
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
