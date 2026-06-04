package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.Representante;

import java.util.List;
import java.util.UUID;

public interface PersonaDonanteServiceInterface {
  void registrar(PersonaDonante donante);
  PersonaDonante obtenerPorId(UUID id);
  void darDeBaja(UUID id);
  void reactivar(UUID id);
  void agregarMedioDeContacto(UUID id, MedioDeContacto medio);
  void agregarRepresentante(UUID id, Representante representante);
  void removerRepresentante(UUID id, String emailRepresentante);
  List<PersonaDonante> listarDonantesActivos();
  List<PersonaDonante> listarTodosDonantes();
}
