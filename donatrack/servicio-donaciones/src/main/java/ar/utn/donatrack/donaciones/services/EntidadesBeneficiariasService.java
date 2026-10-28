package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.mappers.RepresentanteMapper;
import ar.utn.donatrack.donaciones.mappers.DireccionMapper;
import ar.utn.donatrack.donaciones.mappers.ContactoMapper;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.dtos.request.CampaniaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntidadBeneficiariaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.NecesidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CampaniaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadConDonacionesException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.validations.entidades.EntidadesBeneficiariasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Transactional
@Service
@RequiredArgsConstructor
public class EntidadesBeneficiariasService {

    private final RepositorioEntidadesBeneficiarias repositorio;
    private final RepositorioDonaciones donaciones;
    private final EntidadesBeneficiariasValidator validador;
    private final EntidadBeneficiariaMapper mapper;
    private final DireccionMapper direcciones;
    private final ContactoMapper contactos;
    private final RepresentanteMapper representantes;

    public EntidadBeneficiariaResponseDTO guardar(EntidadBeneficiariaRequestDTO dto) {
        EntidadBeneficiaria entidad = mapper.toModel(dto);
        repositorio.guardar(entidad);
        return mapper.toDTO(entidad);
    }

    @Transactional(readOnly = true)
    public List<EntidadBeneficiariaResponseDTO> obtenerTodas() {
        return repositorio.buscarTodas().stream().map(mapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public EntidadBeneficiariaResponseDTO obtenerPorId(UUID id) {
        return mapper.toDTO(validador.validarYObtenerEntidad(id));
    }

    public void actualizar(UUID id, EntidadBeneficiariaRequestDTO dtoNueva) {
        EntidadBeneficiaria entidadExistente = validador.validarYObtenerEntidad(id);

        entidadExistente.setRazonSocial(dtoNueva.getRazonSocial());
        entidadExistente.setDireccion(direcciones.toDireccion(dtoNueva.getDireccion()));

        if (dtoNueva.getContactos() != null) {
            entidadExistente.reemplazarContactos(contactos.toContactos(dtoNueva.getContactos()));
        }
        if (dtoNueva.getRepresentantes() != null) {
            entidadExistente.reemplazarRepresentantes(representantes.toRepresentantes(dtoNueva.getRepresentantes()));
        }

        repositorio.guardar(entidadExistente);
    }

    // NUEVO REQUERIDO: Eliminar entidad
    public void eliminarEntidad(UUID id) {
        EntidadBeneficiaria entidad = validador.validarYObtenerEntidad(id);
        // Sin FK entre donacion y entidad_beneficiaria: la integridad la cuida el
        // servicio. Borrar la entidad dejaría donaciones apuntando a nadie.
        if (donaciones.existenAsignadasA(id)) {
            throw new EntidadConDonacionesException(id);
        }
        repositorio.eliminar(entidad);
    }

    // NUEVO REQUERIDO: Crear Campaña dentro de Entidad
    public CampaniaResponseDTO agregarCampaniaAEntidad(UUID idEntidad, CampaniaRequestDTO dto) {
        EntidadBeneficiaria entidad = validador.validarYObtenerEntidad(idEntidad);
        validador.validarFechasCampania(dto.getFechaInicio(), dto.getFechaFin());
        Campania nuevaCampania = mapper.toCampaniaModel(dto, idEntidad);

        entidad.agregarCampania(nuevaCampania);
        repositorio.guardar(entidad);

        return mapper.toCampaniaDTO(nuevaCampania);
    }

    // ACTUALIZADO CON DTO
    public NecesidadResponseDTO agregarNecesidadACampania(UUID entidadId, UUID campaniaId, NecesidadRequestDTO dto) {
        EntidadBeneficiaria entidad = validador.validarYObtenerEntidad(entidadId);

        Campania campaniaTarget = validador.validarYObtenerCampania(entidad, campaniaId);

        Necesidad nuevaNecesidad = dto.crear(FechaHoraArgentina.hoy());
        campaniaTarget.agregarNecesidad(nuevaNecesidad);

        repositorio.guardar(entidad);

        return mapper.toNecesidadDTO(nuevaNecesidad);
    }

    @Transactional(readOnly = true)
    public List<NecesidadResponseDTO> obtenerNecesidadesDeCampania(UUID entidadId, UUID campaniaId) {
        EntidadBeneficiaria entidad = validador.validarYObtenerEntidad(entidadId);
        Campania campania = validador.validarYObtenerCampania(entidad, campaniaId);

        return campania.getNecesidades().stream().map(mapper::toNecesidadDTO).toList();
    }

    @Transactional(readOnly = true)
    public NecesidadResponseDTO obtenerNecesidad(UUID entidadId, UUID campaniaId, UUID necesidadId) {
        EntidadBeneficiaria entidad = validador.validarYObtenerEntidad(entidadId);
        Campania campania = validador.validarYObtenerCampania(entidad, campaniaId);

        return mapper.toNecesidadDTO(validador.validarYObtenerNecesidad(campania, necesidadId));
    }

    public NecesidadResponseDTO actualizarNecesidad(UUID entidadId, UUID campaniaId, UUID necesidadId,
                                                    NecesidadRequestDTO dto) {
        EntidadBeneficiaria entidad = validador.validarYObtenerEntidad(entidadId);
        Campania campania = validador.validarYObtenerCampania(entidad, campaniaId);
        Necesidad necesidad = validador.validarYObtenerNecesidad(campania, necesidadId);

        // Rechaza el cambio de tipo (EXTRAORDINARIA <-> RECURRENTE)
        dto.aplicarEn(necesidad);

        repositorio.guardar(entidad);

        return mapper.toNecesidadDTO(necesidad);
    }

    public void eliminarNecesidad(UUID entidadId, UUID campaniaId, UUID necesidadId) {
        EntidadBeneficiaria entidad = validador.validarYObtenerEntidad(entidadId);
        Campania campania = validador.validarYObtenerCampania(entidad, campaniaId);

        // Valida primero para responder 404 si no existe, en vez de un borrado silencioso
        validador.validarYObtenerNecesidad(campania, necesidadId);
        campania.eliminarNecesidad(necesidadId);

        repositorio.guardar(entidad);
    }
}