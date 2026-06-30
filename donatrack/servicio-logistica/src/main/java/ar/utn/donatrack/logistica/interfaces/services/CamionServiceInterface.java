package ar.utn.donatrack.logistica.interfaces.services;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CamionServiceInterface {
    CamionResponseDTO registrar(CamionRequestDTO dto);
    List<CamionResponseDTO> obtenerTodos();
    CamionResponseDTO obtenerPorId(UUID id);
}
