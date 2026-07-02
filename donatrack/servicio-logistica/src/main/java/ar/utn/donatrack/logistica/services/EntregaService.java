package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.ConfirmarEntregaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.NoRecibidaRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.exceptions.EntregaNoEncontradaException;
import ar.utn.donatrack.logistica.integracion.EntregaEventPublisher;
import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.services.EntregaServiceInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.validations.EntregaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntregaService implements EntregaServiceInterface {

    private final EntregaRepositoryInterface repositorio;
    private final EntregaValidator validador;
    private final EntregaEventPublisher eventPublisher;

    @Override
    public EntregaResponseDTO obtenerPorId(UUID id) {
        return EntregaResponseDTO.desde(buscarOFallar(id));
    }

    @Override
    public List<EntregaResponseDTO> obtenerPorEstado(EstadoEntrega estado) {
        return repositorio.buscarPorEstado(estado).stream().map(EntregaResponseDTO::desde).toList();
    }

    @Override
    public void confirmar(UUID id, ConfirmarEntregaRequestDTO dto) {
        Entrega entrega = buscarOFallar(id);
        validador.validarTransicion(entrega.getEstado(), EstadoEntrega.ENTREGADA);

        entrega.getFotosComprobante().addAll(dto.getFotosComprobante());
        entrega.setFechaEntrega(LocalDateTime.now());
        entrega.registrarCambio(EstadoEntrega.ENTREGADA, null);
        repositorio.guardar(entrega);

        eventPublisher.publicar(EntregaEvento.builder()
                .tipo(TipoEventoLogistica.ENTREGA_CONFIRMADA)
                .entregaId(entrega.getId())
                .idDonacion(entrega.getIdDonacion())
                .idEntidadBeneficiaria(entrega.getIdEntidadBeneficiaria())
                .idDonante(entrega.getIdDonante())
                .camionId(entrega.getCamion() != null ? entrega.getCamion().getId() : null)
                .rutaId(entrega.getRuta() != null ? entrega.getRuta().getId() : null)
                .fotosComprobante(entrega.getFotosComprobante())
                .build());
    }

    @Override
    public void marcarNoRecibida(UUID id, NoRecibidaRequestDTO dto) {
        Entrega entrega = buscarOFallar(id);
        validador.validarTransicion(entrega.getEstado(), EstadoEntrega.NO_RECIBIDA);

        entrega.registrarCambio(EstadoEntrega.NO_RECIBIDA, dto.getMotivo());
        repositorio.guardar(entrega);

        eventPublisher.publicar(EntregaEvento.builder()
                .tipo(TipoEventoLogistica.ENTREGA_NO_RECIBIDA)
                .entregaId(entrega.getId())
                .idDonacion(entrega.getIdDonacion())
                .idEntidadBeneficiaria(entrega.getIdEntidadBeneficiaria())
                .idDonante(entrega.getIdDonante())
                .camionId(entrega.getCamion() != null ? entrega.getCamion().getId() : null)
                .rutaId(entrega.getRuta() != null ? entrega.getRuta().getId() : null)
                .motivo(dto.getMotivo())
                .build());
    }

    @Override
    public void regresarADeposito(UUID id) {
        Entrega entrega = buscarOFallar(id);
        validador.validarTransicion(entrega.getEstado(), EstadoEntrega.PENDIENTE);
        entrega.registrarCambio(EstadoEntrega.PENDIENTE, "Regreso a depósito");
        repositorio.guardar(entrega);
    }

    private Entrega buscarOFallar(UUID id) {
        Entrega entrega = repositorio.buscarPorId(id);
        if (entrega == null) {
            throw new EntregaNoEncontradaException(id);
        }
        return entrega;
    }
}
