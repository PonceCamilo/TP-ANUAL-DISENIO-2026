package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.ConfirmarEntregaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.NoRecibidaRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.exceptions.EntregaNoEncontradaException;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.integracion.EntregaEventPublisher;
import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.services.EntregaServiceInterface;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.entrega.MotivoFalloEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
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
    private final RutaRepositoryInterface rutaRepositorio;
    private final EntregaValidator validador;
    private final EntregaEventPublisher eventPublisher;
    private final PlanificacionServiceInterface planificacionService;

    @Override
    public EntregaResponseDTO obtenerPorId(UUID id) {
        Entrega entrega = buscarOFallar(id);
        return EntregaResponseDTO.desde(entrega, rutaDe(entrega.getId()));
    }

    @Override
    public List<EntregaResponseDTO> obtenerPorEstado(EstadoEntrega estado) {
        return repositorio.buscarPorEstado(estado).stream()
                .map(e -> EntregaResponseDTO.desde(e, rutaDe(e.getId())))
                .toList();
    }

    @Override
    public void confirmar(UUID id, ConfirmarEntregaRequestDTO dto) {
        Entrega entrega = buscarOFallar(id);
        validador.validarTransicion(entrega.getEstado(), EstadoEntrega.ENTREGADA);

        entrega.getFotosComprobante().addAll(dto.getFotosComprobante());
        entrega.setFechaEntrega(LocalDateTime.now());
        entrega.registrarCambio(EstadoEntrega.ENTREGADA, null);
        repositorio.guardar(entrega);

        Ruta ruta = buscarRutaDeEntregaOFallar(entrega.getId());
        Camion camion = ruta.getCamion();
        eventPublisher.publicar(EntregaEvento.builder()
                .tipo(TipoEventoLogistica.ENTREGA_CONFIRMADA)
                .entregaId(entrega.getId())
                .idDonacion(entrega.getIdDonacion())
                .idEntidadBeneficiaria(idEntidadDe(entrega))
                .idCamion(camion != null ? camion.getId() : null)
                .patenteCamion(camion != null ? camion.getPatente() : null)
                .fechaHoraEntrega(entrega.getFechaEntrega())
                .rutaId(ruta.getId())
                .fotosComprobante(entrega.getFotosComprobante())
                .build());

        finalizarRutaSiCorresponde(ruta.getId());
    }

    @Override
    public void marcarNoRecibida(UUID id, NoRecibidaRequestDTO dto) {
        Entrega entrega = buscarOFallar(id);
        validador.validarTransicion(entrega.getEstado(), EstadoEntrega.NO_RECIBIDA);

        MotivoFalloEntrega motivo = dto.getMotivo();
        entrega.registrarCambio(EstadoEntrega.NO_RECIBIDA, motivo.name());
        repositorio.guardar(entrega);

        Ruta ruta = buscarRutaDeEntregaOFallar(entrega.getId());
        Camion camion = ruta.getCamion();
        eventPublisher.publicar(EntregaEvento.builder()
                .tipo(TipoEventoLogistica.ENTREGA_NO_RECIBIDA)
                .entregaId(entrega.getId())
                .idDonacion(entrega.getIdDonacion())
                .idEntidadBeneficiaria(idEntidadDe(entrega))
                .idCamion(camion != null ? camion.getId() : null)
                .patenteCamion(camion != null ? camion.getPatente() : null)
                .rutaId(ruta.getId())
                .motivoFallo(motivo.name())
                .replanificable(motivo.esReplanificable())
                .build());

        finalizarRutaSiCorresponde(ruta.getId());
    }

    @Override
    public void regresarADeposito(UUID id) {
        Entrega entrega = buscarOFallar(id);
        validador.validarTransicion(entrega.getEstado(), EstadoEntrega.PENDIENTE);
        entrega.registrarCambio(EstadoEntrega.PENDIENTE, "Regreso a depósito");
        repositorio.guardar(entrega);
    }

    // Cuando esta entrega era la última EN_TRASLADO de su ruta, el camión
    // vuelve a estar DISPONIBLE para la planificación del día siguiente.
    private void finalizarRutaSiCorresponde(UUID rutaId) {
        planificacionService.finalizarRutaSiCorresponde(rutaId);
    }

    private Entrega buscarOFallar(UUID id) {
        Entrega entrega = repositorio.buscarPorId(id);
        if (entrega == null) {
            throw new EntregaNoEncontradaException(id);
        }
        return entrega;
    }

    private Ruta buscarRutaDeEntregaOFallar(UUID entregaId) {
        return rutaRepositorio.buscarPorEntregaId(entregaId)
                .orElseThrow(() -> RutaNoEncontradaException.paraEntrega(entregaId));
    }

    private Ruta rutaDe(UUID entregaId) {
        return rutaRepositorio.buscarPorEntregaId(entregaId).orElse(null);
    }

    private UUID idEntidadDe(Entrega entrega) {
        return entrega.getParada() != null ? entrega.getParada().getIdEntidadBeneficiaria() : null;
    }
}
