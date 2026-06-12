package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.PersonaDonanteServiceInterface;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;

import ar.utn.donatrack.donaciones.validations.PersonasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonaDonanteService implements PersonaDonanteServiceInterface {

    private final PersonaDonanteRepositoryInterface repositorio;
    private final PersonasValidator validadorPersonas;

    public void registrar(PersonaDonante donante) {

        repositorio.guardar(donante);
    }

    public PersonaDonante obtenerPersona(UUID idPersona, String mail) {
        return repositorio.obtenerPersona(idPersona, mail);
    }

    public List<PersonaDonante> obtenerPersonasDonantes() {
        return repositorio.obtenerTodosDonantes();
    }

    public List<PersonaDonante> obtenerDonantesPorEstado(EstadoDonante estado) {
        validadorPersonas.validarExistenciaEstado(estado);

        return repositorio.obtenerTodosDonantes().stream()
                .filter(d -> d.getEstado() == estado)
                .toList();
    }

    public void cambiarEstadoPersona(UUID id, EstadoDonante estado) {
        repositorio.cambiarEstado(id, estado);
    }

    public void modificarMedioContacto(UUID id, MedioDeContacto medio) {
        repositorio.modificarMedioContacto(id, medio);
    }

    public void agregarMedioDeContacto(UUID id, MedioDeContacto medio) {
        PersonaDonante persona = repositorio.obtenerPersona(id, null);
        persona.getContactos().add(medio);
        repositorio.guardar(persona);
    }

    public void modificarRepresentante(UUID idPersonaJuridica, Representante representante) {
        repositorio.modificarRepresentante(idPersonaJuridica, representante);
    }
}
