package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.dtos.request.EstadoDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.Representante;

import java.util.List;
import java.util.UUID;

public interface PersonaDonanteServiceInterface {
  UUID registrar(PersonaDonanteRequestDTO dto);
  PersonaDonanteResponseDTO obtenerDonante(UUID id);
  List<PersonaDonanteResponseDTO> obtenerDonantes(EstadoDonante estado);
  void cambiarEstado(UUID id, EstadoDonanteRequestDTO dto);
  void modificarContacto(UUID id, MedioDeContactoRequestDTO dto);
  void modificarRepresentante(UUID id, RepresentanteRequestDTO dto);
}
