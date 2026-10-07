package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CallbackParadaDTO;
import ar.utn.donatrack.logistica.dtos.request.CallbackRutaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.CallbackVehiculoRutaDTO;
import ar.utn.donatrack.logistica.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.DonacionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.PlanificacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaPlanificadaProveedorDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.exceptions.CamionNoEncontradoException;
import ar.utn.donatrack.logistica.exceptions.LoteCallbackInvalidoException;
import ar.utn.donatrack.logistica.exceptions.LoteNoEncontradoException;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.exceptions.SinCamionesDisponiblesException;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Facade: oculta detrás de planificar()/registrarCallback() el particionado
 * en lotes de ≤100 donaciones, el reparto de esas donaciones entre los
 * camiones disponibles, la planificación síncrona (una llamada al proveedor
 * externo por camión) y la traducción de su respuesta (Strategy:
 * EstrategiaRuteoPort) en Ruta/Parada/Entrega.
 */
@Service
@Transactional
public class PlanificacionRutasService implements PlanificacionServiceInterface {

    // Link simulado al mapa de seguimiento en tiempo real; se completa con el id de ruta.
    private static final String BASE_URL_MAPA_INTERACTIVO = "https://donatrack.org/tracking/ruta/";

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
        List<Camion> camiones = resolverCamiones(dto.getCamionesIds());
        List<DonacionLote> donaciones = dto.getDonaciones().stream().map(this::aDonacionLote).toList();

        return particionar(donaciones, maxDonacionesPorLote).stream()
                .map(batch -> crearYPlanificarLote(batch, camiones))
                .map(resultado -> LoteResponseDTO.desde(resultado.lote(), resultado.rutas()))
                .toList();
    }

    @Override
    public LoteResponseDTO obtenerLote(UUID loteId) {
        return LoteResponseDTO.desde(buscarLoteOFallar(loteId));
    }

    @Override
    public void registrarCallback(CallbackRutaRequestDTO dto, String tokenCorrelacion) {
        LotePlanificacion lote = buscarLoteOFallar(dto.getLoteId());
        if (!lote.getTokenCorrelacion().equals(tokenCorrelacion)) {
            throw new LoteCallbackInvalidoException(lote.getId());
        }

        for (CallbackVehiculoRutaDTO vehiculo : dto.getRutas()) {
            Camion camion = camionRepositorio.buscarPorId(vehiculo.getCamionId());
            if (camion == null) {
                throw new CamionNoEncontradoException(vehiculo.getCamionId());
            }
            crearRuta(lote, camion, vehiculo.getParadas());
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

        List<UUID> idsDonaciones = new ArrayList<>();
        for (Entrega entrega : ruta.obtenerEntregas()) {
            entregaValidator.validarTransicion(entrega.getEstado(), EstadoEntrega.EN_TRASLADO);
            entrega.registrarCambio(EstadoEntrega.EN_TRASLADO, "Inicio de ruta");
            entregaRepositorio.guardar(entrega);
            idsDonaciones.add(entrega.getIdDonacion());
        }

        // Un solo evento por ruta agrupando todas sus donaciones: Donaciones
        // resuelve los contactos de cada una y dispara el aviso con el link al mapa.
        if (!idsDonaciones.isEmpty()) {
            eventPublisher.publicar(EntregaEvento.builder()
                    .tipo(TipoEventoLogistica.INICIO_RUTA)
                    .rutaId(ruta.getId())
                    .idsDonaciones(idsDonaciones)
                    .urlMapaInteractivo(BASE_URL_MAPA_INTERACTIVO + ruta.getId())
                    .build());
        }
    }

    @Override
    public void finalizarRutaSiCorresponde(UUID rutaId) {
        Ruta ruta = buscarRutaOFallar(rutaId);
        if (ruta.getEstado() != EstadoRuta.INICIADA) {
            return;
        }

        boolean quedanEntregasEnCurso = ruta.obtenerEntregas().stream()
                .anyMatch(entrega -> entrega.getEstado() == EstadoEntrega.EN_TRASLADO);
        if (quedanEntregasEnCurso) {
            return;
        }

        ruta.setEstado(EstadoRuta.FINALIZADA);
        rutaRepositorio.guardar(ruta);

        Camion camion = ruta.getCamion();
        if (camion != null) {
            camion.setEstado(EstadoCamion.DISPONIBLE);
            camionRepositorio.guardar(camion);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private record ResultadoLote(LotePlanificacion lote, List<Ruta> rutas) {
    }

    /**
     * Crea el lote y, de a un camión por vez, le pide al proveedor externo
     * (Strategy: EstrategiaRuteoPort) que planifique la porción de donaciones
     * que le corresponde. Cada llamada es síncrona: el proveedor devuelve la
     * ruta planificada en el mismo response, así que al terminar el lote ya
     * tiene todas sus rutas armadas y queda COMPLETADO.
     */
    private ResultadoLote crearYPlanificarLote(List<DonacionLote> batch, List<Camion> camiones) {
        LotePlanificacion lote = LotePlanificacion.builder()
                .id(UUID.randomUUID())
                .camiones(camiones)
                .donaciones(batch)
                .estado(EstadoLote.ENVIADO)
                .tokenCorrelacion(UUID.randomUUID().toString())
                .fechaEnvio(LocalDateTime.now())
                .build();
        loteRepositorio.guardar(lote);

        Map<Camion, List<DonacionLote>> donacionesPorCamion = distribuirEntreCamiones(batch, camiones);
        List<Ruta> rutas = new ArrayList<>();
        for (Camion camion : camiones) {
            List<DonacionLote> donacionesDelCamion = donacionesPorCamion.get(camion);
            if (donacionesDelCamion.isEmpty()) {
                continue;
            }
            RutaPlanificadaProveedorDTO rutaPlanificada = estrategiaRuteo.planificarParaCamion(lote, camion, donacionesDelCamion);
            rutas.add(crearRuta(lote, camion, rutaPlanificada.getParadas()));
        }

        lote.setEstado(EstadoLote.COMPLETADO);
        lote.setFechaRespuesta(LocalDateTime.now());
        loteRepositorio.guardar(lote);

        return new ResultadoLote(lote, rutas);
    }

    /** Reparto simple round-robin: cada donación va al camión (índice % cantidad de camiones). */
    private Map<Camion, List<DonacionLote>> distribuirEntreCamiones(List<DonacionLote> donaciones, List<Camion> camiones) {
        Map<Camion, List<DonacionLote>> porCamion = new LinkedHashMap<>();
        for (Camion camion : camiones) {
            porCamion.put(camion, new ArrayList<>());
        }
        for (int i = 0; i < donaciones.size(); i++) {
            Camion camion = camiones.get(i % camiones.size());
            porCamion.get(camion).add(donaciones.get(i));
        }
        return porCamion;
    }

    private Ruta crearRuta(LotePlanificacion lote, Camion camion, List<CallbackParadaDTO> paradasDTO) {
        List<Parada> paradas = new ArrayList<>();

        Ruta ruta = Ruta.builder()
                .id(UUID.randomUUID())
                .lote(lote)
                .camion(camion)
                .paradas(paradas)
                .estado(EstadoRuta.PLANIFICADA)
                .build();
        List<Entrega> entregasDeLaRuta = new ArrayList<>();

        for (CallbackParadaDTO paradaDTO : paradasDTO) {
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
                Entrega entrega = Entrega.builder()
                        .id(UUID.randomUUID())
                        .idDonacion(idDonacion)
                        .parada(parada)
                        .build();
                entregas.add(entrega);
                entregasDeLaRuta.add(entrega);
            }
        }

        // Primero la ruta: guarda en cascada sus paradas y las entregas de cada
        // parada. Una entrega no puede guardarse antes que la parada a la que pertenece.
        rutaRepositorio.guardar(ruta);
        entregasDeLaRuta.forEach(entregaRepositorio::guardar);
        return ruta;
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

    /** Si no se indicaron camiones, se planifica con toda la flota DISPONIBLE. */
    private List<Camion> resolverCamiones(List<UUID> camionesIds) {
        if (camionesIds != null && !camionesIds.isEmpty()) {
            return buscarCamionesOFallar(camionesIds);
        }
        List<Camion> disponibles = camionRepositorio.buscarTodos().stream()
                .filter(camion -> camion.getEstado() == EstadoCamion.DISPONIBLE)
                .toList();
        if (disponibles.isEmpty()) {
            throw new SinCamionesDisponiblesException();
        }
        return disponibles;
    }

    private List<Camion> buscarCamionesOFallar(List<UUID> camionesIds) {
        List<Camion> camiones = camionRepositorio.buscarPorIds(camionesIds);
        UUID idFaltante = camionesIds.stream()
                .filter(id -> camiones.stream().noneMatch(camion -> camion.getId().equals(id)))
                .findFirst()
                .orElse(null);
        if (idFaltante != null) {
            throw new CamionNoEncontradoException(idFaltante);
        }
        return camiones;
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
