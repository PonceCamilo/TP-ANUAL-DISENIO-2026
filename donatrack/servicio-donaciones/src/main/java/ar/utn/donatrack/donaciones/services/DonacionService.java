package ar.utn.donatrack.donaciones.services;

import org.springframework.transaction.annotation.Transactional;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionConBienesVencidosException;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosPendientesResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.repositories.FiltroDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import ar.utn.donatrack.donaciones.validations.entidades.EntidadesBeneficiariasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional
@Service
@RequiredArgsConstructor
public class DonacionService {

  private static final String ESTADO_EN_DEPOSITO = "EN_DEPOSITO";

  private final RepositorioDonaciones repositorio;
  private final DonacionMapper mapper;
  private final AsignacionDonacionesService asignacionService;
  private final RepositorioEntidadesBeneficiarias entidadesRepositorio;
  private final EntidadBeneficiariaMapper entidadMapper;
  private final RepositorioPersonasDonantes donanteRepositorio;
  private final Notificador notificador;
  private final IncentivosClient incentivosClient;
  private final DonacionesValidator validador;
  private final EntidadesBeneficiariasValidator entidadesValidator;
  private final RepositorioCandidatosAsignacion candidatosRepositorio;

  /**
   * Listado paginado. Los filtros se resuelven en la base (Entrega 5): antes se
   * cargaban todas las donaciones con sus bienes e historial y se filtraba en
   * memoria, así que el costo crecía con el total de donaciones del sistema.
   */
  @Transactional(readOnly = true)
  public PaginaResponseDTO<DonacionResponseDTO> obtenerDonaciones(FiltroDonaciones filtro, SolicitudPagina pagina) {
    return PaginaResponseDTO.desde(repositorio.buscar(filtro, pagina).map(mapper::toDTO));
  }

  @Transactional(readOnly = true)
  public DonacionResponseDTO obtenerPorId(UUID id) {
    return mapper.toDTO(validador.validarYObtenerDonacion(id));
  }

  public void cambiarEstado(UUID id, CambioEstadoRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(id);
    boolean estabaEnDeposito = donacion.estaEnEstado(ESTADO_EN_DEPOSITO);
    donacion.cambiarEstadoManualmente(dto.getEstado(), dto.getNombreTransicion(), dto.getJustificacion());
    repositorio.guardar(donacion);

    // Si dejó el depósito (por ejemplo, venció), su ranking ya no sirve: sale de
    // la bandeja ahora y no recién en el matchmaking de la noche siguiente.
    if (estabaEnDeposito && !donacion.estaEnEstado(ESTADO_EN_DEPOSITO)) {
      candidatosRepositorio.eliminar(id);
    }

    if (donacion.fueEntregada()) {
      notificarDonacionExitosa(donacion);
    }
  }

  /** Corrige un bien puntual de la donación (ver Donacion.reemplazarBien para las reglas). */
  public DonacionResponseDTO modificarBien(UUID id, UUID idBien, BienRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(id);
    donacion.reemplazarBien(idBien, mapper.toBien(dto));
    repositorio.guardar(donacion);
    return mapper.toDTO(donacion);
  }

  /**
   * Endpoint anterior (PATCH /donaciones/{id}/bien): reemplazaba siempre el
   * primer bien. Ahora solo funciona si la donación tiene un único bien; con
   * varios hay que indicar cuál.
   */
  public void modificarUnicoBien(UUID id, BienRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(id);
    donacion.reemplazarUnicoBien(mapper.toBien(dto));
    repositorio.guardar(donacion);
  }

  @Transactional(readOnly = true)
  public CandidatosAsignacionResponseDTO obtenerCandidatos(UUID idDonacion) {
    Donacion donacion = validador.validarYObtenerDonacion(idDonacion);
    rechazarSiTieneBienesVencidos(donacion);
    AsignacionDonacionesService.ResultadoMatchmaking ranking = asignacionService.generarRanking(donacion);
    Map<UUID, EntidadBeneficiariaResponseDTO> entidades = cargarEntidades(List.of(
        ranking.getRankingSemantico(), ranking.getRankingSubAtendidos(), ranking.getCoincidencias()));

    return CandidatosAsignacionResponseDTO.builder()
        .idDonacion(idDonacion)
        .porCompatibilidad(mapearRanking(ranking.getRankingSemantico(), entidades))
        .porSubatendidos(mapearRanking(ranking.getRankingSubAtendidos(), entidades))
        .coincidencias(mapearRanking(ranking.getCoincidencias(), entidades))
        .build();
  }

  /**
   * Devuelve las donaciones en depósito con las entidades que les recomendó el
   * último matchmaking nocturno (AsignacionBatchService), para que una persona
   * administradora confirme el destino final.
   *
   * Las entidades de todos los rankings se cargan en una sola consulta: antes se
   * buscaba cada entidad por separado, y con 30 donaciones en la bandeja eran
   * miles de consultas para armar una sola respuesta.
   */
  @Transactional(readOnly = true)
  public List<CandidatosPendientesResponseDTO> obtenerCandidatosPendientes() {
    List<RankingPrecalculado> rankings = candidatosRepositorio.obtenerTodos();
    Map<UUID, EntidadBeneficiariaResponseDTO> entidades = cargarEntidades(rankings.stream()
        .flatMap(r -> Stream.of(r.getCoincidencias(), r.getRankingSemantico(), r.getRankingSubAtendidos()))
        .toList());

    return rankings.stream()
        .map(ranking -> CandidatosPendientesResponseDTO.builder()
            .idDonacion(ranking.getIdDonacion())
            .fechaCalculo(ranking.getFechaCalculo())
            .huboCoincidencias(ranking.huboCoincidencias())
            .coincidencias(mapearRanking(ranking.getCoincidencias(), entidades))
            .porCompatibilidad(mapearRanking(ranking.getRankingSemantico(), entidades))
            .porSubatendidos(mapearRanking(ranking.getRankingSubAtendidos(), entidades))
            .build())
        .toList();
  }

  public void asignar(UUID idDonacion, AsignacionRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(idDonacion);
    rechazarSiTieneBienesVencidos(donacion);
    EntidadBeneficiaria entidad = entidadesValidator.validarYObtenerEntidad(dto.getIdEntidadBeneficiaria());

    donacion.asignarA(entidad);
    // Guarda la asignación y el contador de recepciones de la entidad en una
    // sola transacción; la notificación sale recién después de confirmarla.
    repositorio.guardarAsignacion(donacion);

    // Ya tiene destino: sale de la bandeja de pendientes del administrador.
    candidatosRepositorio.eliminar(idDonacion);

    notificador.donacionAsignada(donanteRepositorio.obtenerPersona(donacion.getIdDonante()), entidad);
  }

  public void eliminar(UUID id) {
    validador.validarYObtenerDonacion(id);
    repositorio.eliminar(id);
    // Sin su donación, el ranking precalculado quedaría huérfano en la bandeja.
    candidatosRepositorio.eliminar(id);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private static void rechazarSiTieneBienesVencidos(Donacion donacion) {
    if (donacion.tieneBienesVencidosAl(FechaHoraArgentina.hoy())) {
      throw new DonacionConBienesVencidosException(donacion.getId());
    }
  }

  /** Carga en una sola consulta todas las entidades que aparecen en los rankings. */
  private Map<UUID, EntidadBeneficiariaResponseDTO> cargarEntidades(List<List<ResultadoAsignacion>> rankings) {
    Set<UUID> ids = rankings.stream()
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .map(ResultadoAsignacion::getIdEntidad)
        .collect(Collectors.toSet());
    if (ids.isEmpty()) {
      return Map.of();
    }
    return entidadesRepositorio.obtenerPorIds(ids).stream()
        .collect(Collectors.toMap(EntidadBeneficiaria::getId, entidadMapper::toDTO));
  }

  /**
   * Convierte un ranking de (idEntidad, puntaje) en los DTOs de las entidades,
   * respetando el orden del ranking. Si una entidad se borró después del
   * cálculo, se omite.
   */
  private List<EntidadBeneficiariaResponseDTO> mapearRanking(List<ResultadoAsignacion> ranking,
                                                             Map<UUID, EntidadBeneficiariaResponseDTO> entidades) {
    if (ranking == null) {
      return List.of();
    }
    return ranking.stream()
        .map(resultado -> entidades.get(resultado.getIdEntidad()))
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Avisa a incentivos que la donación llegó a destino (actualiza donaciones
   * exitosas / organizaciones ayudadas). En un cambio manual a ENTREGADA, la
   * fecha de entrega es la del cambio.
   */
  private void notificarDonacionExitosa(Donacion donacion) {
    notificador.destinoDe(donanteRepositorio.obtenerPersona(donacion.getIdDonante())).ifPresent(destino ->
        incentivosClient.notificarDonacionExitosa(donacion, destino.valor(), destino.canal().name(),
            FechaHoraArgentina.ahora()));
  }
}
