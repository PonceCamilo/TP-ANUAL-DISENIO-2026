package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.exceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.RepresentanteEnPersonaNoJuridicaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.PersonaDonanteServiceInterface;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonaDonanteService implements PersonaDonanteServiceInterface {

    private final PersonaDonanteRepositoryInterface repositorio;

    /**
     * Registra una nueva PersonaDonante.
     * Valida unicidad de email antes de delegar al repositorio.
     */
    public void registrar(PersonaDonante donante) {
        if (donante.getEmail() != null
            && repositorio.obtenerPorMail(donante.getEmail()) != null) {
            throw new EmailYaRegistradoException();
        }
        repositorio.guardar(donante);
    }

    public PersonaDonante obtenerPorId(UUID id) {
        return repositorio.obtenerPorId(id);
    }

    public void darDeBaja(UUID id) {
        repositorio.darDeBaja(id);
    }

    public void reactivar(UUID id) {
        repositorio.reactivar(id);
    }

    public List<PersonaDonante> listarDonantesActivos() {
        return repositorio.obtenerTodosActivos();
    }

    public List<PersonaDonante> listarTodosDonantes() {
        return repositorio.obtenerTodosDonantes();
    }

    public void agregarMedioDeContacto(UUID id, MedioDeContacto medio) {
        PersonaDonante persona = repositorio.obtenerPorId(id);
        persona.getContactos().add(medio);
        repositorio.guardar(persona); // necesario cuando haya DB
    }

    public void agregarRepresentante(UUID id, Representante representante) {
        if (!(repositorio.obtenerPorId(id) instanceof PersonaJuridica persona)) {
            throw new RepresentanteEnPersonaNoJuridicaException();
        }
        persona.getRepresentantes().add(representante);
    }

    public void removerRepresentante(UUID id, String emailRepresentante) {
        if (!(repositorio.obtenerPorId(id) instanceof PersonaJuridica persona)) {
            throw new RepresentanteEnPersonaNoJuridicaException();
        }
        persona.getRepresentantes().removeIf(rep ->
            rep.getEmail() != null && rep.getEmail().equalsIgnoreCase(emailRepresentante)
        );
    }
}
