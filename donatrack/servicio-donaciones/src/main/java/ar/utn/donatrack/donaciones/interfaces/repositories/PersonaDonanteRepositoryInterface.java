package ar.utn.donatrack.donaciones.interfaces.repositories;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.Representante;

import java.util.List;
import java.util.UUID;

/**
 * Interfaz mínima para persistir y buscar donantes desde el servicio de importación.
 * Implementaciones existentes del proyecto pueden adaptarse a esta interfaz.
 */
public interface PersonaDonanteRepositoryInterface {
    PersonaDonante obtenerPersona(UUID idPersona, String mail);
    List<PersonaDonante> obtenerTodosDonantes();
    void guardar(PersonaDonante donante);
    void cambiarEstado(UUID idDonante, EstadoDonante nuevoEstado);
    void modificarRepresentante(UUID idDonante, Representante representante);
    void modificarMedioContacto(UUID idDonante, MedioDeContacto medio);
    boolean existePorId(UUID id);
    boolean existePorEmail(String email);
    PersonaDonante obtenerPersona(UUID id); // firma simplificada
}
