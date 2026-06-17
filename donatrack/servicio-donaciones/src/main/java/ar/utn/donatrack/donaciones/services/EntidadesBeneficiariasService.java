package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.CampaniaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntidadBeneficiariaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.NecesidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.EntidadesBeneficiariasServiceInterface;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.mappers.PersonaDonanteMapper;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.validations.EntidadesBeneficiariasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntidadesBeneficiariasService implements EntidadesBeneficiariasServiceInterface {

    private final EntidadesBeneficiariasRepositoryInterface repositorio;
    private final EntidadesBeneficiariasValidator validador;
    private final EntidadBeneficiariaMapper mapper;
    private final PersonaDonanteMapper dependenciasMapper;

    public UUID guardar(EntidadBeneficiariaRequestDTO dto) {
        EntidadBeneficiaria entidad = mapper.toModel(dto);
        repositorio.guardar(entidad);
        return entidad.getId();
    }

    public List<EntidadBeneficiariaResponseDTO> obtenerTodas() {
        return repositorio.buscarTodas().stream().map(mapper::toDTO).toList();
    }

    public EntidadBeneficiariaResponseDTO obtenerPorId(UUID id) {
        validador.validarExistenciaEntidad(id);
        return mapper.toDTO(repositorio.obtenerPorId(id));
    }

    public void actualizar(UUID id, EntidadBeneficiariaRequestDTO dtoNueva) {
        validador.validarExistenciaEntidad(id);
        EntidadBeneficiaria entidadExistente = repositorio.obtenerPorId(id);

        entidadExistente.setRazonSocial(dtoNueva.getRazonSocial());
        entidadExistente.setDireccion(dependenciasMapper.toDireccion(dtoNueva.getDireccion()));

        if (dtoNueva.getContactos() != null) {
            entidadExistente.setContactos(dtoNueva.getContactos().stream()
                    .map(dependenciasMapper::toContacto).toList());
        }
        if (dtoNueva.getRepresentantes() != null) {
            entidadExistente.setRepresentantes(dependenciasMapper.toRepresentantes(dtoNueva.getRepresentantes()));
        }

        repositorio.guardar(entidadExistente);
    }

    // NUEVO REQUERIDO: Eliminar entidad (Podría ser baja lógica según tu repositorio)
    public void eliminarEntidad(UUID id) {
        validador.validarExistenciaEntidad(id);
        // Suponiendo que tu repo tenga un método eliminar o que uses borrado lógico
        // repositorio.eliminar(id);
    }

    // NUEVO REQUERIDO: Crear Campaña dentro de Entidad
    public UUID agregarCampaniaAEntidad(UUID idEntidad, CampaniaRequestDTO dto) {
        validador.validarExistenciaEntidad(idEntidad);
        validador.validarFechasCampania(dto.getFechaInicio(), dto.getFechaFin());

        EntidadBeneficiaria entidad = repositorio.obtenerPorId(idEntidad);
        Campania nuevaCampania = mapper.toCampaniaModel(dto, idEntidad);

        entidad.agregarCampania(nuevaCampania);
        repositorio.guardar(entidad);

        return nuevaCampania.getIdCampania();
    }

    // ACTUALIZADO CON DTO
    public void agregarNecesidadACampania(UUID entidadId, UUID campaniaId, NecesidadRequestDTO dto) {
        validador.validarExistenciaEntidad(entidadId);
        EntidadBeneficiaria entidad = repositorio.obtenerPorId(entidadId);

        Campania campaniaTarget = validador.validarYObtenerCampania(entidad, campaniaId);

        // Transformamos el DTO a la Necesidad real de forma segura
        Necesidad nuevaNecesidad = mapper.toNecesidadModel(dto);
        campaniaTarget.getNecesidades().add(nuevaNecesidad);

        repositorio.guardar(entidad);
    }
}