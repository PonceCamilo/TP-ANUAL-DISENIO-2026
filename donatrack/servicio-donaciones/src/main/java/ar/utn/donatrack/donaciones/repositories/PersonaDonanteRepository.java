package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;

import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import ar.utn.donatrack.donaciones.validations.PersonasValidator;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.lang.annotation.Repeatable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria para PersonaDonante.
 * Spring garantiza scope singleton con @Repository: una única instancia
 * compartida en todo el contexto de la aplicación.
 *
 * El índice secundario `porEmail` permite O(1) para la búsqueda por email,
 * necesaria tanto en el registro individual como en la importación masiva CSV.
 */
@Repository
@Getter
public class PersonaDonanteRepository implements PersonaDonanteRepositoryInterface {

    private PersonasValidator validadorPersonas;
    private final Map<UUID, PersonaDonante> almacenamiento = new ConcurrentHashMap<>();
    private final Map<String, UUID> indicePorEmail = new ConcurrentHashMap<>();


    public void guardar(PersonaDonante personaDonante) {
        if (personaDonante.getId() == null) {
            personaDonante.setId(UUID.randomUUID());
        }

        almacenamiento.put(personaDonante.getId(), personaDonante);

        indicePorEmail.put(personaDonante.getEmail().toLowerCase(), personaDonante.getId());
    }

    public PersonaDonante obtenerPersona(UUID idPersona, String mail) {
        validadorPersonas.validarExistenciaPersona(idPersona, mail);

        if(idPersona != null){
            return almacenamiento.get(idPersona);
        } else {
            UUID id = indicePorEmail.get(mail.toLowerCase());
            return almacenamiento.get(id);
        }
    }

    public List<PersonaDonante> obtenerTodosDonantes() {
        return almacenamiento.values().stream().toList();
    }

    public void cambiarEstado(UUID id, EstadoDonante nuevoEstado) {
        validadorPersonas.validarExistenciaPersona(id, null);
        validadorPersonas.validarCambioEstado(obtenerPersona(id, null).getEstado(), nuevoEstado);

        PersonaDonante persona = obtenerPersona(id, null);

        persona.setEstado(nuevoEstado);
    }

    public void modificarRepresentante(UUID idPersonaJuridica, Representante representante) {
        validadorPersonas.validarExistenciaPersona(idPersonaJuridica, null);
        validadorPersonas.validarTipoPersona(idPersonaJuridica);

        PersonaJuridica persona = (PersonaJuridica) obtenerPersona(idPersonaJuridica, null);

        boolean eliminado = persona.getRepresentantes()
            .removeIf(rep -> rep.getEmail().equalsIgnoreCase(representante.getEmail()));

        // 2. Si no eliminó nada, significa que no estaba, por lo tanto lo agregamos
        if (!eliminado) {
            persona.getRepresentantes().add(representante);
        }
    }

    public void modificarMedioContacto(UUID id, MedioDeContacto medio) {
        validadorPersonas.validarMedioContacto(medio);

        PersonaDonante persona = obtenerPersona(id, null);


        boolean eliminado = persona.getContactos()
            .removeIf(mc -> mc.getValor().equalsIgnoreCase(medio.getValor()));

        // 2. Si no eliminó nada, significa que no estaba, por lo tanto lo agregamos
        if (!eliminado) {
            persona.getContactos().add(medio);
        }
    }
}
