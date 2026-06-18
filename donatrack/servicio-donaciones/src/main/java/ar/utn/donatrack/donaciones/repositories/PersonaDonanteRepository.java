package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.Representante;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Getter
public class PersonaDonanteRepository implements PersonaDonanteRepositoryInterface {

    private final Map<UUID, PersonaDonante> almacenamiento = new ConcurrentHashMap<>();
    private final Map<String, UUID> indicePorEmail = new ConcurrentHashMap<>();

    public void guardar(PersonaDonante personaDonante) {
        if (personaDonante.getId() == null) {
            personaDonante.setId(UUID.randomUUID());
        }
        almacenamiento.put(personaDonante.getId(), personaDonante);
        indicePorEmail.put(personaDonante.getEmail().toLowerCase(), personaDonante.getId());
    }

    public PersonaDonante obtenerPersona(UUID id) {
        return almacenamiento.get(id);
    }

    public List<PersonaDonante> obtenerTodosDonantes() {
        return almacenamiento.values().stream().toList();
    }

    public void cambiarEstado(UUID id, EstadoDonante nuevoEstado) {
        PersonaDonante persona = obtenerPersona(id);
        if (persona != null) {
            persona.setEstado(nuevoEstado);
        }
    }

    public List<PersonaDonante> obtenerInactivosDesde(LocalDateTime fechaLimite) {
        return almacenamiento.values().stream()
                .filter(p -> p.getUltimaInteraccion() == null
                        || p.getUltimaInteraccion().isBefore(fechaLimite))
                .toList();
    }

    public void modificarMedioContacto(UUID id, MedioDeContacto medio) {
        PersonaDonante persona = obtenerPersona(id);
        if (persona != null) {
            Class<?> tipoNuevo = medio.getClass();
            persona.getContactos().removeIf(mc -> mc.getClass().equals(tipoNuevo));
            persona.getContactos().add(medio);
        }
    }

    public void modificarRepresentante(UUID idPersonaJuridica, Representante representante) {
        PersonaJuridica persona = (PersonaJuridica) obtenerPersona(idPersonaJuridica);
        if (persona != null) {
            persona.getRepresentantes()
                    .removeIf(rep -> rep.getEmail() != null
                            && rep.getEmail().equalsIgnoreCase(representante.getEmail()));
            persona.getRepresentantes().add(representante);
        }
    }

    public boolean existePorId(UUID id) {
        return almacenamiento.containsKey(id);
    }

    public boolean existePorEmail(String email) {
        return indicePorEmail.containsKey(email.toLowerCase());
    }

    public PersonaDonante obtenerPorEmail(String email) {
        UUID id = indicePorEmail.get(email.toLowerCase());
        return id != null ? almacenamiento.get(id) : null;
    }

    public List<PersonaDonante> obtenerPorEstado(EstadoDonante estado) {
        return almacenamiento.values().stream()
                .filter(p -> p.getEstado() == estado)
                .toList();
    }

    public void eliminar(UUID id) {
        PersonaDonante persona = almacenamiento.remove(id);
        if (persona != null && persona.getEmail() != null) {
            indicePorEmail.remove(persona.getEmail().toLowerCase());
        }
    }
}