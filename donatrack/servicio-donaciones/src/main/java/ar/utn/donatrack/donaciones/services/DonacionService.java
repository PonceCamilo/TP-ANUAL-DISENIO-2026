package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.DonacionServiceInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

  public List<DonacionResponseDTO> obtenerDonaciones(String estado, UUID idDonante, String subcategoria) {
    List<Donacion> resultado = repositorio.obtenerTodas();

    if (estado != null && !estado.isBlank()) {
      resultado = resultado.stream().filter(d -> d.getEstado().nombre().equals(estado)).toList();
    }
    if (idDonante != null) {
      resultado = resultado.stream().filter(d -> idDonante.equals(d.getIdDonante())).toList();
    }
    if (subcategoria != null && !subcategoria.isBlank()) {
      resultado = resultado.stream()
          .filter(d -> d.getSubcategoria() != null
              && d.getSubcategoria().getTipo() != null
              && d.getSubcategoria().getTipo().equalsIgnoreCase(subcategoria))
          .toList();
    }
    return mapper.toDTOList(resultado);
  }

  public DonacionResponseDTO obtenerPorId(UUID id) {
    return mapper.toDTO(buscarOFallar(id));
  }

  public void cambiarEstado(UUID id, CambioEstadoRequestDTO dto) {
    Donacion donacion = buscarOFallar(id);
    donacion.cambiarEstado(dto.getEstado(), dto.getNombreTransicion(), dto.getJustificacion());

    if ("ENTREGADA".equals(donacion.getEstado().nombre())) {
      notificarDonacionExitosa(donacion);
    }
  }

  public void modificarBien(UUID id, BienRequestDTO dto) {
    Donacion donacion = buscarOFallar(id);
    if (!donacion.getBienes().isEmpty()) {
      donacion.getBienes().set(0, mapper.toBien(dto));
    }
  }

  public CandidatosAsignacionResponseDTO obtenerCandidatos(UUID idDonacion) {
    Donacion donacion = buscarOFallar(idDonacion);
    AsignacionDonacionesService.ResultadoMatchmaking ranking = asignacionService.generarRanking(donacion);

    return CandidatosAsignacionResponseDTO.builder()
        .idDonacion(idDonacion)
        .porCompatibilidad(mapearRanking(ranking.getRankingSemantico()))
        .porSubatendidos(mapearRanking(ranking.getRankingSubAtendidos()))
        .coincidencias(mapearRanking(ranking.getCoincidencias()))
        .build();
  }

  public void asignar(UUID idDonacion, AsignacionRequestDTO dto) {
    Donacion donacion = buscarOFallar(idDonacion);

    EntidadBeneficiaria entidad = entidadesRepositorio.obtenerPorId(dto.getIdEntidadBeneficiaria());
    if (entidad == null) {
      throw new EntidadBeneficiariaNoEncontradaException(dto.getIdEntidadBeneficiaria());
    }

    donacion.setIdEntidadBeneficiaria(entidad.getId());
    donacion.setFechaAsignacion(LocalDate.now());
    donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", "Asignada a " + entidad.getRazonSocial());

    notificarAsignacion(donacion, entidad);
  }

  public void eliminar(UUID id) {
    buscarOFallar(id);
    repositorio.eliminar(id);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  /** Recupera la donación o lanza la excepción de dominio (mismo patrón que PersonaDonanteService). */
  private Donacion buscarOFallar(UUID id) {
    Donacion donacion = repositorio.obtenerPorId(id);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(id);
    }
    return donacion;
  }

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
    if (donante != null && donante.getEmail() != null) {
      incentivosClient.notificarDonacionExitosa(donacion.getIdDonante(), donante.getEmail(), "EMAIL");
    }
  }

  private void notificarAsignacion(Donacion donacion, EntidadBeneficiaria entidad) {
    String emailEntidad = obtenerEmail(entidad.getContactos());
    if (emailEntidad != null) {
      notificacionClient.enviarNotificacion(
              emailEntidad,
              "Se te asignó una nueva donación según tus necesidades registradas.",
              "EMAIL",
              "ASIGNACION_DONACION_ENTIDAD"
      );
    }

    PersonaDonante donante = donanteRepositorio.obtenerPersona(donacion.getIdDonante());
    if (donante != null && donante.getEmail() != null) {
      notificacionClient.enviarNotificacion(
              donante.getEmail(),
              "Tu donación fue asignada a " + entidad.getRazonSocial() + ".",
              "EMAIL",
              "ASIGNACION_DONACION_DONANTE"
      );
    }
  }

  private String obtenerEmail(List<MedioDeContacto> contactos) {
    if (contactos == null) {
      return null;
    }
    return contactos.stream()
            .filter(c -> c instanceof Email)
            .map(MedioDeContacto::getValor)
            .findFirst()
            .orElse(null);
  }
}
