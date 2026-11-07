package ar.utn.donatrack.logistica.interfaces.services;

import ar.utn.donatrack.logistica.dtos.request.CallbackRutaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.PlanificacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PlanificacionServiceInterface {
    List<LoteResponseDTO> planificar(PlanificacionRequestDTO dto);

    /**
     * Arma rutas nuevas para las entregas que volvieron al depósito y admiten
     * otra salida. No recibe un pedido de Donaciones.
     */
    List<LoteResponseDTO> replanificarPendientes();
    LoteResponseDTO obtenerLote(UUID loteId);
    void registrarCallback(CallbackRutaRequestDTO dto, String tokenCorrelacion);
    RutaResponseDTO obtenerRuta(UUID rutaId);
    RutaResponseDTO obtenerRutaVigentePorCamion(UUID camionId);
    void iniciarRuta(UUID rutaId);
    void finalizarRutaSiCorresponde(UUID rutaId);
}
