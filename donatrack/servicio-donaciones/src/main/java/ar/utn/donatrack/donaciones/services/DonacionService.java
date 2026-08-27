package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.DonacionServiceInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import ar.utn.donatrack.donaciones.validations.entidades.EntidadesBeneficiariasValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonacionService implements DonacionServiceInterface {

  private final DonacionesRepositoryInterface repositorio;
  private final DonacionMapper mapper;
  private final AsignacionDonacionesService asignacionService;
  private final EntidadesBeneficiariasRepositoryInterface entidadesRepositorio;
  private final EntidadBeneficiariaMapper entidadMapper;
  private final PersonaDonanteRepositoryInterface donanteRepositorio;
  private final NotificacionClient notificacionClient;
  private final IncentivosClient incentivosClient;
  private final DonacionesValidator validador;
  private final EntidadesBeneficiariasValidator entidadesValidator;

  public List<DonacionResponseDTO> obtenerDonaciones(String estado, UUID idDonante, String subcategoria) {
    List<Donacion> resultado = repositorio.obtenerTodas();

    if (estado != null && !estado.isBlank()) {
      resultado = resultado.stream().filter(d -> d.estaEnEstado(estado)).toList();
    }
    if (idDonante != null) {
      resultado = resultado.stream().filter(d -> idDonante.equals(d.getIdDonante())).toList();
    }
    if (subcategoria != null && !subcategoria.isBlank()) {
      resultado = resultado.stream().filter(d -> d.esDeSubcategoria(subcategoria)).toList();
    }
    return mapper.toDTOList(resultado);
  }

  public DonacionResponseDTO obtenerPorId(UUID id) {
    return mapper.toDTO(validador.validarYObtenerDonacion(id));
  }

  public void cambiarEstado(UUID id, CambioEstadoRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(id);
    donacion.cambiarEstado(dto.getEstado(), dto.getNombreTransicion(), dto.getJustificacion());

    if (donacion.fueEntregada()) {
      notificarDonacionExitosa(donacion);
    }
  }

  public void modificarBien(UUID id, BienRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(id);
    validador.validarTieneBienes(donacion);
    donacion.getBienes().set(0, mapper.toBien(dto));
  }

  public CandidatosAsignacionResponseDTO obtenerCandidatos(UUID idDonacion) {
    Donacion donacion = validador.validarYObtenerDonacion(idDonacion);
    AsignacionDonacionesService.ResultadoMatchmaking ranking = asignacionService.generarRanking(donacion);

    return CandidatosAsignacionResponseDTO.builder()
        .idDonacion(idDonacion)
        .porCompatibilidad(mapearRanking(ranking.getRankingSemantico()))
        .porSubatendidos(mapearRanking(ranking.getRankingSubAtendidos()))
        .coincidencias(mapearRanking(ranking.getCoincidencias()))
        .build();
  }

  public void asignar(UUID idDonacion, AsignacionRequestDTO dto) {
    Donacion donacion = validador.validarYObtenerDonacion(idDonacion);
    EntidadBeneficiaria entidad = entidadesValidator.validarYObtenerEntidad(dto.getIdEntidadBeneficiaria());

    donacion.asignarA(entidad);

    notificarAsignacion(donacion, entidad);
  }

  public void eliminar(UUID id) {
    validador.validarYObtenerDonacion(id);
    repositorio.eliminar(id);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  /** Convierte un ranking de (idEntidad, puntaje) en los DTOs de las entidades correspondientes. */
  private List<EntidadBeneficiariaResponseDTO> mapearRanking(List<ResultadoAsignacion> ranking) {
    return ranking.stream()
        .map(resultado -> entidadesRepositorio.obtenerPorId(resultado.getIdEntidad()))
        .filter(Objects::nonNull)
        .map(entidadMapper::toDTO)
        .toList();
  }

  /** Avisa a incentivos que la donación llegó a destino (actualiza donaciones exitosas / organizaciones ayudadas). */
  private void notificarDonacionExitosa(Donacion donacion) {
    PersonaDonante donante = donanteRepositorio.obtenerPersona(donacion.getIdDonante());
    if (donante != null && donante.obtenerEmail() != null) {
      incentivosClient.notificarDonacionExitosa(donacion, donante.obtenerEmail(), "EMAIL");
    }
  }

  private void notificarAsignacion(Donacion donacion, EntidadBeneficiaria entidad) {
    String emailEntidad = entidad.obtenerEmail();
    if (emailEntidad != null) {
      notificacionClient.enviarNotificacion(
              emailEntidad,
              "Se te asignó una nueva donación según tus necesidades registradas.",
              "EMAIL"
      );
    }

    PersonaDonante donante = donanteRepositorio.obtenerPersona(donacion.getIdDonante());
    if (donante != null && donante.obtenerEmail() != null) {
      notificacionClient.enviarNotificacion(
              donante.obtenerEmail(),
              "Tu donación fue asignada a " + entidad.getRazonSocial() + ".",
              "EMAIL"
      );
    }
  }
}
