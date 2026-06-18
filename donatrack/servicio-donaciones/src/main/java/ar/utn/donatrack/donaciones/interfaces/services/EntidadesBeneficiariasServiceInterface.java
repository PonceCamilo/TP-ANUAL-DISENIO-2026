package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.dtos.request.CampaniaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntidadBeneficiariaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.NecesidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;

import java.util.List;
import java.util.UUID;

public interface EntidadesBeneficiariasServiceInterface {
    UUID guardar(EntidadBeneficiariaRequestDTO dto);
    List<EntidadBeneficiariaResponseDTO> obtenerTodas();
    EntidadBeneficiariaResponseDTO obtenerPorId(UUID id);
    void actualizar(UUID id, EntidadBeneficiariaRequestDTO dto);
    void eliminarEntidad(UUID id);
    UUID agregarCampaniaAEntidad(UUID idEntidad, CampaniaRequestDTO dto);
    UUID agregarNecesidadACampania(UUID entidadId, UUID campaniaId, NecesidadRequestDTO dto);
}
