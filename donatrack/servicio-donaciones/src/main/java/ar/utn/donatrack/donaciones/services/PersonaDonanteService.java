package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.excepcion.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.excepcion.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.repositories.PersonaDonanteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PersonaDonanteService {

    private final PersonaDonanteRepository repositorio;

    public PersonaDonanteService(PersonaDonanteRepository repositorio) {
        this.repositorio = repositorio;
    }

    public PersonaDonante registrar(PersonaDonante donante) {
        if (repositorio.existePorEmail(
                donante.getMedioContactoPredeterminado().getValor())) {
            throw new EmailYaRegistradoException(
                donante.getMedioContactoPredeterminado().getValor());
        }
        repositorio.guardar(donante);
        return donante;
    }

    public PersonaDonante buscarPorId(UUID id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new PersonaDonanteNoEncontradaException(id));
    }

    public void darDeBaja(UUID id) {
        PersonaDonante donante = buscarPorId(id);
        donante.darDeBaja();
        repositorio.guardar(donante);
    }

    public void reactivar(UUID id) {
        PersonaDonante donante = buscarPorId(id);
        donante.reactivar();
        repositorio.guardar(donante);
    }

    public List<PersonaDonante> listarActivos() {
        return repositorio.buscarActivos();
    }

    public List<PersonaDonante> listarTodos() {
        return repositorio.buscarTodos();
    }
}