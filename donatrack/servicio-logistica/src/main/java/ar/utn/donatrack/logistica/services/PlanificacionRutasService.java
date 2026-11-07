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
import ar.utn.donatrack.logistica.exceptions.ProveedorRuteoIndisponibleException;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.exceptions.SinCamionesDisponiblesException;
import ar.utn.donatrack.logistica.integracion.EntregaEventPublisher;
import ar.utn.donatrack.logistica.interfaces.integracion.EstrategiaRuteoPort;
import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Facade: oculta detrás de planificar()/registrarCallback() el particionado
 * en lotes de ≤100 donaciones, la planificación síncrona (una llamada al
 * proveedor externo por lote, con todas las donaciones y todos los camiones)
 * y la traducción de su respuesta (Strategy: EstrategiaRuteoPort) en
 * Ruta/Parada/Entrega. Quién lleva cada donación lo decide el proveedor.
 *
 * replanificarPendientes() usa el mismo camino, pero no recibe un pedido de
 * Donaciones: las donaciones ya están en una ruta terminada y volvieron al depósito.
 */
@Service
@Transactional
public class PlanificacionRutasService implements PlanificacionServiceInterface {

    // Link simulado al mapa de seguimiento en tiempo real; se completa con el id de ruta.
    private static final String BASE_URL_MAPA_INTERACTIVO = "https://donatrack.org/tracking/ruta/";

    private final LotePlanificacionRepositoryInterface loteRepositorio;
    private final RutaRepositoryInterface rutaRepositorio;
    private final CamionRepositoryInterface camionRepositorio;
    private final EstrategiaRuteoPort estrategiaRuteo;
    private final EntregaEventPublisher eventPublisher;
    private final EntregaValidator entregaValidator;
    private final int maxDonacionesPorLote;

    public PlanificacionRutasService(
            LotePlanificacionRepositoryInterface loteRepositorio,
            RutaRepositoryInterface rutaRepositorio,
            CamionRepositoryInterface camionRepositorio,
            EstrategiaRuteoPort estrategiaRuteo,
            EntregaEventPublisher eventPublisher,
            EntregaValidator entregaValidator,
            @Value("${integraciones.proveedor-ruteo.max-donaciones-por-lote}") int maxDonacionesPorLote) {
        this.loteRepositorio = loteRepositorio;
        this.rutaRepositorio = rutaRepositorio;
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

    /**
     * Segunda salida de las donaciones que el administrador ya devolvió al depósito.
     * Donaciones no manda este pedido: la donación sigue en ENTREGA_FALLIDA hasta
     * que el chofer inicie la ruta nueva y salga el aviso de siempre.
     *
     * El lote que devolvemos es nuevo. 
     * El lote anterior y la ruta del intento fallido quedan sin cambios.
     */
    @Override
    public List<LoteResponseDTO> replanificarPendientes() {
        List<Pendiente> pendientes = pendientesDeReplanificar();
        if (pendientes.isEmpty()) {
            return List.of();
        }

        // Misma flota que un planificar sin camionesIds: los que están DISPONIBLE.
        List<Camion> camiones = resolverCamiones(null);
        // Si la misma donación quedó dos veces en el depósito, al proveedor va una sola.
        List<DonacionLote> donaciones = pendientes.stream()
                .filter(Pendiente::esLaQueSeEnvia)
                .map(this::aDonacionLote)
                .toList();

        List<ResultadoLote> resultados = particionar(donaciones, maxDonacionesPorLote).stream()
                .map(batch -> crearYPlanificarLote(batch, camiones))
                .toList();

        // Para anotar en la entrega vieja en qué ruta nueva quedó su donación
        Map<UUID, UUID> rutaNuevaPorDonacion = new HashMap<>();
        for (ResultadoLote resultado : resultados) {
            for (Ruta ruta : resultado.rutas()) {
                for (Entrega entrega : ruta.obtenerEntregas()) {
                    rutaNuevaPorDonacion.put(entrega.getIdDonacion(), ruta.getId());
                }
            }
        }

        // La entrega vieja vuelve a NO_RECIBIDA. Si siguiera LISTO_PARA_ENTREGAR,
        // habría dos entregas "en curso" de la misma donación.
        // Varias entregas pueden vivir en la misma ruta: se guarda una vez por ruta.
        Set<Ruta> rutasViejas = new HashSet<>();
        for (Pendiente pendiente : pendientes) {
            UUID rutaNueva = rutaNuevaPorDonacion.get(pendiente.entrega().getIdDonacion());
            if (rutaNueva == null) {
                throw new IllegalStateException(
                        "La donación " + pendiente.entrega().getIdDonacion() + " no quedó en una ruta nueva.");
            }
            pendiente.entrega().cerrarPorReplanificacion(rutaNueva);
            rutasViejas.add(pendiente.ruta());
        }
        rutasViejas.forEach(rutaRepositorio::guardar);

        return resultados.stream()
                .map(resultado -> LoteResponseDTO.desde(resultado.lote(), resultado.rutas()))
                .toList();
    }

    /**
     * Quién puede salir de nuevo: 
     * 1. está LISTO_PARA_ENTREGAR,    
     * 2. su ruta ya terminó,
     * 3. el motivo admite otra ruta y
     * 4. esa donación no está en una ruta que todavía
     *           no finalizó (planificada o en camino).
     */
    private List<Pendiente> pendientesDeReplanificar() {
        Set<UUID> enRutaAbierta = new HashSet<>();
        for (Ruta ruta : rutaRepositorio.buscarPorEstado(EstadoRuta.PLANIFICADA)) {
            ruta.obtenerEntregas().forEach(entrega -> enRutaAbierta.add(entrega.getIdDonacion()));
        }
        for (Ruta ruta : rutaRepositorio.buscarPorEstado(EstadoRuta.INICIADA)) {
            ruta.obtenerEntregas().forEach(entrega -> enRutaAbierta.add(entrega.getIdDonacion()));
        }

        List<Pendiente> pendientes = new ArrayList<>();
        Set<UUID> yaEnviadas = new HashSet<>();
        for (Ruta ruta : rutaRepositorio.buscarPorEstado(EstadoRuta.FINALIZADA)) {
            for (Entrega entrega : ruta.obtenerEntregas()) {
                if (!entrega.listaParaReplanificar() || enRutaAbierta.contains(entrega.getIdDonacion())) {
                    continue;
                }
                // add devuelve false si esa donación ya estaba: esa copia no se le manda
                // al proveedor, pero igual se cierra cuando exista la ruta nueva.
                boolean esLaQueSeEnvia = yaEnviadas.add(entrega.getIdDonacion());
                pendientes.add(new Pendiente(ruta, entrega, esLaQueSeEnvia));
            }
        }
        return pendientes;
    }

    /** La dirección ya está en la parada del intento anterior: no se le vuelve a pedir a Donaciones. */
    private DonacionLote aDonacionLote(Pendiente pendiente) {
        Parada parada = pendiente.entrega().getParada();
        if (parada == null || parada.getDireccion() == null || parada.getIdEntidadBeneficiaria() == null) {
            throw new IllegalStateException(
                    "La entrega " + pendiente.entrega().getId() + " no tiene dirección para replanificar.");
        }
        return DonacionLote.builder()
                .idDonacion(pendiente.entrega().getIdDonacion())
                .idEntidadBeneficiaria(parada.getIdEntidadBeneficiaria())
                .direccionEntrega(parada.getDireccion())
                .build();
    }

    /** El intento viejo (ruta + entrega) y si esta copia es la que viaja al proveedor. */
    private record Pendiente(Ruta ruta, Entrega entrega, boolean esLaQueSeEnvia) {
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
                .orElseThrow(() -> RutaNoEncontradaException.paraCamion(camionId));
    }

    @Override
    public void iniciarRuta(UUID rutaId) {
        Ruta ruta = buscarRutaOFallar(rutaId);
        ruta.setEstado(EstadoRuta.INICIADA);
        ruta.setFechaInicio(LocalDateTime.now());

        List<UUID> idsDonaciones = new ArrayList<>();
        for (Entrega entrega : ruta.obtenerEntregas()) {
            entregaValidator.validarTransicion(entrega.getEstado(), EstadoEntrega.EN_TRASLADO);
            entrega.registrarCambio(EstadoEntrega.EN_TRASLADO, "Inicio de ruta");
            idsDonaciones.add(entrega.getIdDonacion());
        }
        // Las entregas van embebidas: un solo guardado de la ruta las persiste a todas.
        rutaRepositorio.guardar(ruta);

        Camion camion = ruta.getCamion();
        if (camion != null) {
            camion.setEstado(EstadoCamion.EN_RUTA);
            camionRepositorio.guardar(camion);
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
     * Crea el lote y le pide al proveedor externo (Strategy: EstrategiaRuteoPort)
     * que reparta todas sus donaciones entre los camiones. La llamada es
     * síncrona: el proveedor devuelve una ruta por camión usado. Si esa
     * respuesta no asigna cada donación exactamente una vez, o nombra un
     * camión que no se le envió, el lote se rechaza entero.
     */
    private ResultadoLote crearYPlanificarLote(List<DonacionLote> batch, List<Camion> camiones) {
        LotePlanificacion lote = LotePlanificacion.builder()
                .id(UUID.randomUUID())
                .camionIds(camiones.stream().map(Camion::getId).toList())
                .donaciones(batch)
                .estado(EstadoLote.ENVIADO)
                .tokenCorrelacion(UUID.randomUUID().toString())
                .fechaEnvio(LocalDateTime.now())
                .build();
        loteRepositorio.guardar(lote);

        List<RutaPlanificadaProveedorDTO> rutasPlanificadas = estrategiaRuteo.planificar(lote, camiones, batch);
        validarRespuestaProveedor(batch, camiones, rutasPlanificadas);

        List<Ruta> rutas = new ArrayList<>();
        for (RutaPlanificadaProveedorDTO rutaPlanificada : rutasPlanificadas) {
            Camion camion = null;
            for (Camion candidato : camiones) {
                if (candidato.getId().equals(rutaPlanificada.getCamionId())) {
                    camion = candidato;
                    break;
                }
            }
            if (camion == null) {
                throw new ProveedorRuteoIndisponibleException(
                        "El proveedor de ruteo nombró un camión que no se le envió");
            }
            rutas.add(crearRuta(lote, camion, rutaPlanificada.getParadas()));
        }

        lote.setEstado(EstadoLote.COMPLETADO);
        lote.setFechaRespuesta(LocalDateTime.now());
        loteRepositorio.guardar(lote);

        return new ResultadoLote(lote, rutas);
    }

    /**
     * El proveedor tiene que ubicar cada donación del lote en una sola parada
     * y usar solamente camiones de la lista que recibió. Una respuesta a medias
     * no se persiste: la transacción de planificar() revierte el lote.
     */
    private void validarRespuestaProveedor(List<DonacionLote> donaciones, List<Camion> camiones, List<RutaPlanificadaProveedorDTO> rutas) {
        if (rutas == null) {
            throw new ProveedorRuteoIndisponibleException("El proveedor de ruteo respondió sin rutas");
        }

        Set<UUID> camionesEnviados = new HashSet<>();
        for (Camion camion : camiones) {
            camionesEnviados.add(camion.getId());
        }
        Set<UUID> donacionesEsperadas = new HashSet<>();
        for (DonacionLote donacion : donaciones) {
            donacionesEsperadas.add(donacion.getIdDonacion());
        }

        Set<UUID> camionesVistos = new HashSet<>();
        Set<UUID> donacionesVistas = new HashSet<>();
        for (RutaPlanificadaProveedorDTO ruta : rutas) {
            if (ruta.getCamionId() == null || !camionesEnviados.contains(ruta.getCamionId())) {
                throw new ProveedorRuteoIndisponibleException(
                        "El proveedor de ruteo nombró un camión que no se le envió");
            }
            if (!camionesVistos.add(ruta.getCamionId())) {
                throw new ProveedorRuteoIndisponibleException("El proveedor de ruteo repitió un camión");
            }
            if (ruta.getParadas() == null || ruta.getParadas().isEmpty()) {
                throw new ProveedorRuteoIndisponibleException("El proveedor de ruteo devolvió una ruta sin paradas");
            }
            for (CallbackParadaDTO parada : ruta.getParadas()) {
                if (parada.getDonacionesIds() == null || parada.getDonacionesIds().isEmpty()) {
                    throw new ProveedorRuteoIndisponibleException("El proveedor de ruteo devolvió una parada sin donaciones");
                }
                for (UUID idDonacion : parada.getDonacionesIds()) {
                    if (!donacionesEsperadas.contains(idDonacion) || !donacionesVistas.add(idDonacion)) {
                        throw new ProveedorRuteoIndisponibleException(
                                "El proveedor de ruteo no asignó cada donación del lote exactamente una vez");
                    }
                }
            }
        }
        if (!donacionesVistas.equals(donacionesEsperadas)) {
            throw new ProveedorRuteoIndisponibleException(
                    "El proveedor de ruteo no asignó cada donación del lote exactamente una vez");
        }
    }

    private Ruta crearRuta(LotePlanificacion lote, Camion camion, List<CallbackParadaDTO> paradasDTO) {
        List<Parada> paradas = new ArrayList<>();

        Ruta ruta = Ruta.builder()
                .id(UUID.randomUUID())
                .loteId(lote.getId())
                .camion(camion)
                .paradas(paradas)
                .estado(EstadoRuta.PLANIFICADA)
                .build();

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
            }
        }

        // Un solo documento: la ruta guarda embebidas sus paradas y entregas.
        rutaRepositorio.guardar(ruta);
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
