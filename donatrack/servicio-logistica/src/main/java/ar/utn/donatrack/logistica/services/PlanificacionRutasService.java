package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CallbackParadaDTO;
import ar.utn.donatrack.logistica.dtos.request.CallbackRutaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.CallbackVehiculoRutaDTO;
import ar.utn.donatrack.logistica.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.DonacionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.PlanificacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.exceptions.LoteCallbackInvalidoException;
import ar.utn.donatrack.logistica.exceptions.LoteNoEncontradoException;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.integracion.EntregaEventPublisher;
import ar.utn.donatrack.logistica.interfaces.integracion.EstrategiaRuteoPort;
import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.LotePlanificacionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.planificacion.DonacionLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import ar.utn.donatrack.logistica.models.planificacion.Parada;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import ar.utn.donatrack.logistica.validations.EntregaValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Facade: oculta detrás de planificar()/registrarCallback() el particionado
 * en lotes de ≤100 donaciones, la consulta de la flota y la traducción
 * de la respuesta del proveedor externo (Strategy: EstrategiaRuteoPort)
 * en Ruta/Parada/Entrega.
 */
@Service
public class PlanificacionRutasService implements PlanificacionServiceInterface {

    private final LotePlanificacionRepositoryInterface loteRepositorio;
    private final RutaRepositoryInterface rutaRepositorio;
    private final EntregaRepositoryInterface entregaRepositorio;
    private final CamionRepositoryInterface camionRepositorio;
    private final EstrategiaRuteoPort estrategiaRuteo;
    private final EntregaEventPublisher eventPublisher;
    private final EntregaValidator entregaValidator;
    private final int maxDonacionesPorLote;

    public PlanificacionRutasService(
            LotePlanificacionRepositoryInterface loteRepositorio,
            RutaRepositoryInterface rutaRepositorio,
            EntregaRepositoryInterface entregaRepositorio,
            CamionRepositoryInterface camionRepositorio,
            EstrategiaRuteoPort estrategiaRuteo,
            EntregaEventPublisher eventPublisher,
            EntregaValidator entregaValidator,
            @Value("${integraciones.proveedor-ruteo.max-donaciones-por-lote}") int maxDonacionesPorLote) {
        this.loteRepositorio = loteRepositorio;
        this.rutaRepositorio = rutaRepositorio;
        this.entregaRepositorio = entregaRepositorio;
        this.camionRepositorio = camionRepositorio;
        this.estrategiaRuteo = estrategiaRuteo;
        this.eventPublisher = eventPublisher;
        this.entregaValidator = entregaValidator;
        this.maxDonacionesPorLote = maxDonacionesPorLote;
    }

    @Override
    public List<LoteResponseDTO> planificar(PlanificacionRequestDTO dto) {
        List<Camion> camiones = camionRepositorio.buscarPorIds(dto.getCamionesIds());
        List<DonacionLote> donaciones = dto.getDonaciones().stream().map(this::aDonacionLote).toList();

        List<LotePlanificacion> lotes = particionar(donaciones, maxDonacionesPorLote).stream()
                .map(batch -> crearYEnviarLote(batch, camiones))
                .toList();

        return lotes.stream().map(LoteResponseDTO::desde).toList();
    }

    @Override
    public LoteResponseDTO obtenerLote(UUID loteId) {
        return LoteResponseDTO.desde(buscarLoteOFallar(loteId));
    }

    @Override
    public void registrarCallback(CallbackRutaRequestDTO dto) {
        LotePlanificacion lote = buscarLoteOFallar(dto.getLoteId());
        if (!lote.getTokenCorrelacion().equals(dto.getTokenCorrelacion())) {
            throw new LoteCallbackInvalidoException(lote.getId());
        }

        Map<UUID, DonacionLote> donacionesPorId = lote.getDonaciones().stream()
                .collect(Collectors.toMap(DonacionLote::getIdDonacion, d -> d));

        for (CallbackVehiculoRutaDTO vehiculo : dto.getRutas()) {
            crearRutaDesdeCallback(lote, vehiculo, donacionesPorId);
        }

        lote.setEstado(EstadoLote.COMPLETADO);
        lote.setFechaRespuesta(LocalDateTime.now());
        loteRepositorio.guardar(lote);
    }

    @Override
    public RutaResponseDTO obtenerRuta(UUID rutaId) {
        return RutaResponseDTO.desde(buscarRutaOFallar(rutaId));
    }

    @Override
    public RutaResponseDTO obtenerRutaVigentePorCamion(UUID camionId) {
        return rutaRepositorio.buscarPorCamionId(camionId).stream()
                .filter(r -> r.getEstado() != EstadoRuta.FINALIZADA)
                .findFirst()
                .map(RutaResponseDTO::desde)
                .orElseThrow(() -> new RutaNoEncontradaException(camionId));
    }

    @Override
    public void iniciarRuta(UUID rutaId) {
        Ruta ruta = buscarRutaOFallar(rutaId);
        ruta.setEstado(EstadoRuta.INICIADA);
        ruta.setFechaInicio(LocalDateTime.now());
        rutaRepositorio.guardar(ruta);

        Camion camion = ruta.getCamion();
        if (camion != null) {
            camion.setEstado(EstadoCamion.EN_RUTA);
            camionRepositorio.guardar(camion);
        }

        for (Entrega entrega : entregaRepositorio.buscarPorRutaId(rutaId)) {
            entregaValidator.validarTransicion(entrega.getEstado(), EstadoEntrega.EN_TRASLADO);
            entrega.registrarCambio(EstadoEntrega.EN_TRASLADO, "Inicio de ruta");
            entrega.setCamion(camion);
            entregaRepositorio.guardar(entrega);

            eventPublisher.publicar(EntregaEvento.builder()
                    .tipo(TipoEventoLogistica.INICIO_RUTA)
                    .entregaId(entrega.getId())
                    .idDonacion(entrega.getIdDonacion())
                    .idEntidadBeneficiaria(entrega.getIdEntidadBeneficiaria())
                    .idDonante(entrega.getIdDonante())
                    .camionId(camion != null ? camion.getId() : null)
                    .rutaId(ruta.getId())
                    .build());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private LotePlanificacion crearYEnviarLote(List<DonacionLote> batch, List<Camion> camiones) {
        LotePlanificacion lote = LotePlanificacion.builder()
                .id(UUID.randomUUID())
                .camiones(camiones)
                .donaciones(batch)
                .estado(EstadoLote.ENVIADO)
                .tokenCorrelacion(UUID.randomUUID().toString())
                .fechaEnvio(LocalDateTime.now())
                .build();
        loteRepositorio.guardar(lote);
        estrategiaRuteo.solicitarPlanificacion(lote, camiones);
        return lote;
    }

    private void crearRutaDesdeCallback(LotePlanificacion lote, CallbackVehiculoRutaDTO vehiculo, Map<UUID, DonacionLote> donacionesPorId) {
        List<Parada> paradas = new ArrayList<>();
        Camion camion = camionRepositorio.buscarPorId(vehiculo.getCamionId());

        Ruta ruta = Ruta.builder()
                .id(UUID.randomUUID())
                .lote(lote)
                .camion(camion)
                .paradas(paradas)
                .estado(EstadoRuta.PLANIFICADA)
                .build();

        for (CallbackParadaDTO paradaDTO : vehiculo.getParadas()) {
            List<Entrega> entregas = new ArrayList<>();
            Parada parada = Parada.builder()
                    .id(UUID.randomUUID())
                    .orden(paradaDTO.getOrden())
                    .direccion(aDireccion(paradaDTO.getDireccion()))
                    .idEntidadBeneficiaria(paradaDTO.getIdEntidadBeneficiaria())
                    .entregas(entregas)
                    .build();
            paradas.add(parada);

            for (UUID idDonacion : paradaDTO.getDonacionesIds()) {
                DonacionLote donacionLote = donacionesPorId.get(idDonacion);
                Entrega entrega = Entrega.builder()
                        .id(UUID.randomUUID())
                        .idDonacion(idDonacion)
                        .idEntidadBeneficiaria(paradaDTO.getIdEntidadBeneficiaria())
                        .idDonante(donacionLote != null ? donacionLote.getIdDonante() : null)
                        .parada(parada)
                        .ruta(ruta)
                        .build();
                entregaRepositorio.guardar(entrega);
                entregas.add(entrega);
            }
        }

        rutaRepositorio.guardar(ruta);
    }

    private List<List<DonacionLote>> particionar(List<DonacionLote> donaciones, int tamanioMaximo) {
        List<List<DonacionLote>> lotes = new ArrayList<>();
        for (int i = 0; i < donaciones.size(); i += tamanioMaximo) {
            lotes.add(donaciones.subList(i, Math.min(i + tamanioMaximo, donaciones.size())));
        }
        return lotes;
    }

    private DonacionLote aDonacionLote(DonacionParaRutearRequestDTO dto) {
        return DonacionLote.builder()
                .idDonacion(dto.getIdDonacion())
                .idEntidadBeneficiaria(dto.getIdEntidadBeneficiaria())
                .idDonante(dto.getIdDonante())
                .direccionEntrega(aDireccion(dto.getDireccionEntrega()))
                .build();
    }

    private Direccion aDireccion(DireccionRequestDTO dto) {
        return Direccion.builder()
                .calle(dto.getCalle())
                .numero(dto.getNumero())
                .localidad(dto.getLocalidad())
                .provincia(dto.getProvincia())
                .codigoPostal(dto.getCodigoPostal())
                .build();
    }

    private LotePlanificacion buscarLoteOFallar(UUID id) {
        LotePlanificacion lote = loteRepositorio.buscarPorId(id);
        if (lote == null) {
            throw new LoteNoEncontradoException(id);
        }
        return lote;
    }

    private Ruta buscarRutaOFallar(UUID id) {
        Ruta ruta = rutaRepositorio.buscarPorId(id);
        if (ruta == null) {
            throw new RutaNoEncontradaException(id);
        }
        return ruta;
    }
}
