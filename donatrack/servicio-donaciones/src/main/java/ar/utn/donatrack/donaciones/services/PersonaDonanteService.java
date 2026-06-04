package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.PersonaDonanteServiceInterface;
import ar.utn.donatrack.donaciones.interfaces.services.SegmentadorDonacionesServiceInterface;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.excepcion.EmailYaRegistradoException;
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
    private final SegmentadorDonacionesServiceInterface segmentador;

    public void registrar(PersonaDonante donante) {

        boolean emailExistente = donante.getContactos().stream()
                .filter(contacto -> contacto instanceof Email)
                .map(contacto -> (Email) contacto)
                .anyMatch(contacto -> repositorio.obtenerPorMail(contacto.getDireccion()) != null);

        if (emailExistente) {
            throw new EmailYaRegistradoException("El email ya está registrado");
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


    public void agregarMedioDeContacto(UUID id, MedioDeContacto medio){
        PersonaDonante persona = repositorio.obtenerPorId(id);
        persona.getContactos().add(medio);
    }

    public void agregarRepresentante(UUID id, Representante representante){

        if(!(repositorio.obtenerPorId(id) instanceof PersonaJuridica)){
            throw new IllegalArgumentException("Solo se pueden agregar representantes a personas jurídicas.");
        }
        PersonaJuridica persona = (PersonaJuridica) repositorio.obtenerPorId(id);
        persona.getRepresentantes().add(representante);
    }


    public void removerRepresentante(UUID id, String emailRepresentante){
        PersonaJuridica persona = (PersonaJuridica) repositorio.obtenerPorId(id);
        persona.getRepresentantes().removeIf(rep -> rep.getContactos()
                .stream()
                .filter(contacto -> contacto instanceof Email)
                .map(contacto -> (Email) contacto)
                .anyMatch(mail -> mail.getDireccion().equals(emailRepresentante)));
    }

    public List<PersonaDonante> listarDonantesActivos() {
        return repositorio.obtenerTodosActivos();
    }

    public List<PersonaDonante> listarTodosDonantes() {
        return repositorio.obtenerTodosDonantes();
    }
}
















