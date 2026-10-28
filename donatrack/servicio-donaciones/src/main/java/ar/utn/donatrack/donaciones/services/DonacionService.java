package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.response.RankingAlgoritmoResponseDTO;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionConBienesVencidosException;
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
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import ar.utn.donatrack.donaciones.validations.entidades.EntidadesBeneficiariasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class DonacionService {


  private final RepositorioDonaciones repositorio;
  private final DonacionMapper mapper;
  private final AsignacionDonacionesService asignacionService;
  private final RepositorioEntidadesBeneficiarias entidadesRepositorio;
  private final EntidadBeneficiariaMapper entidadMapper;
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

  /**
   * Cambio de estado manual. Lo que depende del cambio lo hacen los oyentes de
   * sus eventos (paquete eventos): si la donación dejó el depósito su ranking sale
   * de la bandeja, y si llegó a ENTREGADA se imputa a las necesidades de la
   * entidad y se avisa a incentivos.
   */
  public void cambiarEstado(UUID id, CambioEstadoRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(id);
    donacion.cambiarEstadoManualmente(dto.getEstado(), dto.getJustificacion());
    repositorio.guardar(donacion);
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
    ResultadoMatchmaking resultado = asignacionService.generarRanking(donacion);
    Map<UUID, EntidadBeneficiariaResponseDTO> entidades = cargarEntidades(List.copyOf(resultado.rankings().values()));

    return CandidatosAsignacionResponseDTO.builder()
        .idDonacion(idDonacion)
        .rankings(mapearRankings(resultado, entidades))
        .coincidencias(mapearRanking(resultado.coincidencias(), entidades))
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
    Map<UUID, ResultadoMatchmaking> resultados = rankings.stream()
        .collect(Collectors.toMap(RankingPrecalculado::getIdDonacion, RankingPrecalculado::resultado));
    Map<UUID, EntidadBeneficiariaResponseDTO> entidades = cargarEntidades(resultados.values().stream()
        .flatMap(resultado -> resultado.rankings().values().stream())
        .toList());

    return rankings.stream()
        .map(ranking -> {
          ResultadoMatchmaking resultado = resultados.get(ranking.getIdDonacion());
          return CandidatosPendientesResponseDTO.builder()
              .idDonacion(ranking.getIdDonacion())
              .fechaCalculo(ranking.getFechaCalculo())
              .huboCoincidencias(resultado.huboCoincidencias())
              .coincidencias(mapearRanking(resultado.coincidencias(), entidades))
              .rankings(mapearRankings(resultado, entidades))
              .build();
        })
        .toList();
  }

  public void asignar(UUID idDonacion, AsignacionRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(idDonacion);
    rechazarSiTieneBienesVencidos(donacion);
    EntidadBeneficiaria entidad = entidadesValidator.validarYObtenerEntidad(dto.getIdEntidadBeneficiaria());

    donacion.asignarA(entidad);
    // Al guardar, el repositorio también suma la recepción a la entidad, en la
    // misma transacción. Los oyentes sacan la donación de la bandeja y, después
    // de confirmar, avisan a la persona donante y a la entidad.
    repositorio.guardar(donacion);
  }

  /**
   * Baja lógica: la donación queda guardada con su historial y desaparece de las
   * consultas. Al guardarla se descuenta su recepción, y su ranking sale de la
   * bandeja por el evento de baja.
   */
  public void eliminar(UUID id) {
    Donacion donacion = validador.validarYObtenerDonacion(id);
    donacion.darDeBaja();
    repositorio.guardar(donacion);
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

  /** Un elemento por algoritmo, en el orden en que se ejecutaron. */
  private List<RankingAlgoritmoResponseDTO> mapearRankings(ResultadoMatchmaking resultado,
                                                           Map<UUID, EntidadBeneficiariaResponseDTO> entidades) {
    return resultado.rankings().entrySet().stream()
        .map(ranking -> new RankingAlgoritmoResponseDTO(ranking.getKey(), mapearRanking(ranking.getValue(), entidades)))
        .toList();
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

}
