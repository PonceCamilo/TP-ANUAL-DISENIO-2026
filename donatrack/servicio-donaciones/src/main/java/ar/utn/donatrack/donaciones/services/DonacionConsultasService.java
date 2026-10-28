package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosPendientesResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.RankingAlgoritmoResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.FiltroDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Consultas sobre donaciones: listado, detalle, candidatas para asignación y la
 * bandeja de pendientes del matchmaking nocturno.
 *
 * Comandos y consultas van por separado (ver DonacionComandosService): esta
 * clase no modifica nada, y su transacción de solo lectura le avisa a Hibernate
 * que no hay cambios que vigilar. Antes un único DonacionService mezclaba las dos
 * cosas con una docena de colaboradores.
 */
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class DonacionConsultasService {

  private final RepositorioDonaciones repositorio;
  private final DonacionMapper mapper;
  private final AsignacionDonacionesService asignacionService;
  private final RepositorioEntidadesBeneficiarias entidadesRepositorio;
  private final EntidadBeneficiariaMapper entidadMapper;
  private final DonacionesValidator validador;
  private final RepositorioCandidatosAsignacion candidatosRepositorio;

  /**
   * Listado paginado. Los filtros se resuelven en la base (Entrega 5): antes se
   * cargaban todas las donaciones con sus bienes e historial y se filtraba en
   * memoria, así que el costo crecía con el total de donaciones del sistema.
   */
  public PaginaResponseDTO<DonacionResponseDTO> obtenerDonaciones(FiltroDonaciones filtro, SolicitudPagina pagina) {
    return PaginaResponseDTO.desde(repositorio.buscar(filtro, pagina).map(mapper::toDTO));
  }

  public DonacionResponseDTO obtenerPorId(UUID id) {
    return mapper.toDTO(validador.validarYObtenerDonacion(id));
  }

  public CandidatosAsignacionResponseDTO obtenerCandidatos(UUID idDonacion) {
    Donacion donacion = validador.validarYObtenerDonacion(idDonacion);
    donacion.exigirSinBienesVencidosAl(FechaHoraArgentina.hoy());
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

  // ── Helpers ────────────────────────────────────────────────────────────────

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
