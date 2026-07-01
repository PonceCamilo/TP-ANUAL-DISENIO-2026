package ar.utn.donatrack.logistica.interfaces.services;

import ar.utn.donatrack.logistica.dtos.request.ConfirmarEntregaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.NoRecibidaRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;

import java.util.List;
import java.util.UUID;

public interface EntregaServiceInterface {
    EntregaResponseDTO obtenerPorId(UUID id);
    List<EntregaResponseDTO> obtenerPorEstado(EstadoEntrega estado);
    void confirmar(UUID id, ConfirmarEntregaRequestDTO dto);
    void marcarNoRecibida(UUID id, NoRecibidaRequestDTO dto);
    void regresarADeposito(UUID id);
}
