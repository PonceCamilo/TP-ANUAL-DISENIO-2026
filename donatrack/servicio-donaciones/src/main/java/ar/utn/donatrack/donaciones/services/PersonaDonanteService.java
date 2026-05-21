package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.services.PersonaDonanteServiceInterface;
import ar.utn.donatrack.donaciones.model.contacto.TipoMedioContacto;
import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.excepcion.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.model.donante.PersonaJuridicaDonante;
import ar.utn.donatrack.donaciones.model.donante.Representante;
import ar.utn.donatrack.donaciones.repositories.PersonaDonanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonaDonanteService implements PersonaDonanteServiceInterface {

    private final PersonaDonanteRepository repositorio;

    public PersonaDonante registrar(PersonaDonante donante) {
        if (repositorio.existePorEmail(donante.getEmail())) {
            throw new EmailYaRegistradoException(donante.getEmail());
        }
        repositorio.guardar(donante);
        return donante;
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

    public void agregarMedioDeContacto(UUID id, TipoMedioContacto medio){
        PersonaDonante persona = repositorio.obtenerPorId(id);
        persona.getMediosDeContacto().add(medio);
        repositorio.actualizar(persona);
    }

    public void agregarRepresentante(UUID id, Representante representante){

        if(!repositorio.obtenerPorId(id).getTipoPersona().equals("Juridica")){
            throw new IllegalArgumentException("Solo se pueden agregar representantes a personas jurídicas.");
        }
        PersonaJuridicaDonante persona = (PersonaJuridicaDonante) repositorio.obtenerPorId(id);
        persona.getRepresentantes().add(representante);
        repositorio.actualizar(persona);
    }

    public void removerRepresentante(UUID id, String emailRepresentante){
        PersonaJuridicaDonante persona = (PersonaJuridicaDonante) repositorio.obtenerPorId(id);
        persona.getRepresentantes().removeIf(rep -> rep.getEmail().equals(emailRepresentante));
        repositorio.actualizar(persona);
    }

    public List<PersonaDonante> listarDonantesActivos() {
        return repositorio.obtenerTodosActivos();
    }

    public List<PersonaDonante> listarTodosDonantes() {
        return repositorio.obtenerTodosDonantes();
    }
}