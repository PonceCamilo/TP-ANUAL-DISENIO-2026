package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.Representante;

import java.util.List;
import java.util.UUID;

public interface PersonaDonanteServiceInterface {
  void registrar(PersonaDonante donante);
  PersonaDonante obtenerPersona(UUID idPersona, String mail);
  List<PersonaDonante> obtenerPersonasDonantes();
  public List<PersonaDonante> obtenerDonantesPorEstado(EstadoDonante estado);
  void agregarMedioDeContacto(UUID id, MedioDeContacto medio);
  public void modificarRepresentante(UUID idPersonaJuridica, Representante representante);
}
