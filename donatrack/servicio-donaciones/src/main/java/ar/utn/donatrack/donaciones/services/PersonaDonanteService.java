package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.EntidadBeneficiariaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.EntidadesBeneficiariasServiceInterface;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.mappers.PersonaDonanteMapper;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.validations.EntidadesBeneficiariasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return repositorio.buscarTodas().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public EntidadBeneficiariaResponseDTO obtenerPorId(UUID id) {
        validador.validarExistenciaEntidad(id);
        return mapper.toDTO(repositorio.obtenerPorId(id));
    }

    public void actualizar(UUID id, EntidadBeneficiariaRequestDTO dtoNueva) {
        validador.validarExistenciaEntidad(id);
        EntidadBeneficiaria entidadExistente = repositorio.obtenerPorId(id);

        // Actualización completa usando los mappers
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

    // crear NecesidadRequestDTO por NecesidadRequestDTO.
    public void agregarNecesidadACampania(UUID entidadId, UUID campaniaId, Necesidad nuevaNecesidad) {
        validador.validarExistenciaEntidad(entidadId);
        EntidadBeneficiaria entidad = repositorio.obtenerPorId(entidadId);

        // Uso el validador para verificar que la campaña existe dentro de la entidad
        Campania campaniaTarget = validador.validarYObtenerCampania(entidad, campaniaId);
        campaniaTarget.getNecesidades().add(nuevaNecesidad);

        repositorio.guardar(entidad);
    }

    public void actualizarPeriodos() {
        LocalDate hoy = LocalDate.now();
        List<EntidadBeneficiaria> entidades = repositorio.buscarTodas();

        entidades.forEach(entidad ->
                entidad.getCampanias().stream()
                        .flatMap(campania -> campania.getNecesidades().stream())
                        .filter(NecesidadRecurrente.class::isInstance)
                        .map(n -> (NecesidadRecurrente) n)
                        .forEach(nr -> nr.obtenerOGenerarPeriodoActual(hoy))
        );
    }
}