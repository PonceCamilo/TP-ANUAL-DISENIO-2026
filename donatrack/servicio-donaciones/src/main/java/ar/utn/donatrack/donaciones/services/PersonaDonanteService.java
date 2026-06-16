package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.EstadoUpdateDTO;
import ar.utn.donatrack.donaciones.dtos.request.MedioDeContactoDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.PersonaDonanteServiceInterface;
import ar.utn.donatrack.donaciones.mappers.PersonaDonanteMapper;
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
    private final PersonasValidator validador;
    private final PersonaDonanteMapper mapper;

    public UUID registrar(PersonaDonanteRequestDTO dto) {
        validador.validarEmailNoDuplicado(dto.getEmail());
        PersonaDonante donante = mapper.toModel(dto);
        repositorio.guardar(donante);
        return donante.getId();
    }

    public PersonaDonanteResponseDTO obtenerDonante(UUID id) {
        validador.validarExistenciaPersona(id);
        return mapper.toDTO(repositorio.obtenerPersona(id));
    }

    public List<PersonaDonanteResponseDTO> obtenerDonantes(EstadoDonante estado) {
        List<PersonaDonante> todos = repositorio.obtenerTodosDonantes();

        List<PersonaDonante> resultado = estado != null
            ? todos.stream().filter(d -> d.getEstado() == estado).toList()
            : todos;

        return resultado.stream().map(mapper::toDTO).toList();
    }

    public void cambiarEstado(UUID id, EstadoUpdateDTO dto) {
        validador.validarExistenciaPersona(id);
        PersonaDonante donante = repositorio.obtenerPersona(id);
        validador.validarCambioEstado(donante.getEstado(), dto.getEstado());
        repositorio.cambiarEstado(id, dto.getEstado());
    }

    public void modificarContacto(UUID id, MedioDeContactoDTO dto) {
        validador.validarExistenciaPersona(id);
        repositorio.modificarMedioContacto(id, mapper.toContacto(dto));
    }

    public void modificarRepresentante(UUID id, RepresentanteDTO dto) {
        validador.validarExistenciaPersona(id);
        validador.validarEsPersonaJuridica(id);
        repositorio.modificarRepresentante(id, mapper.toRepresentante(dto));
    }
}