package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.EntidadBeneficiariaResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.CambioEstadoDonacionIlegalException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.DonacionServiceInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Whatsapp;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.validations.DonacionesValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonacionService implements DonacionServiceInterface {

  private final DonacionesRepositoryInterface repositorio;
  private final DonacionesValidator validador;
  private final DonacionMapper mapper;
  private final PersonaDonanteRepositoryInterface donanteRepositorio;
  private final EntidadesBeneficiariasRepositoryInterface entidadRepositorio;
  private final EntidadBeneficiariaMapper entidadMapper;
  private final NotificacionClient notificacionClient;

  @Override
  public List<DonacionResponseDTO> obtenerDonaciones(EstadoDonacion estado, UUID idDonante, String subcategoria) {
    List<Donacion> resultado = repositorio.obtenerTodas();

    if (estado != null) {
      resultado = resultado.stream().filter(d -> d.getEstado() == estado).toList();
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

  @Override
  public List<DonacionResponseDTO> obtenerPorDonante(UUID idDonante) {
    return mapper.toDTOList(repositorio.obtenerPorDonante(idDonante));
  }

  @Override
  public DonacionResponseDTO obtenerPorId(UUID id) {
    return mapper.toDTO(buscarOFallar(id));
  }

  @Override
  public void cambiarEstado(UUID id, CambioEstadoRequestDTO dto) {
    Donacion donacion = buscarOFallar(id);
    validador.validarTransicion(donacion.getEstado(), dto.getEstado(), dto.getJustificacion());

    donacion.getHistorialEstados().add(CambioEstado.builder()
        .estado(dto.getEstado())
        .justificacion(dto.getJustificacion())
        .build());
    donacion.setEstado(dto.getEstado());
  }

  @Override
  public void modificarBien(UUID id, BienRequestDTO dto) {
    Donacion donacion = buscarOFallar(id);
    if (donacion.getBienes().isEmpty()) {
      throw new IllegalStateException("La donación no tiene bienes registrados.");
    }
    donacion.getBienes().set(0, mapper.toBien(dto));
  }

  @Override
  public void eliminar(UUID id) {
    buscarOFallar(id);
    repositorio.eliminar(id);
  }

  // ── ASIGNACIÓN ────────────────────────────────────────────────────────────

  @Override
  public CandidatosAsignacionResponseDTO obtenerCandidatos(UUID idDonacion) {
    Donacion donacion = buscarOFallar(idDonacion);
    String sub = donacion.getSubcategoria() != null ? donacion.getSubcategoria().getTipo() : null;
    List<EntidadBeneficiaria> todas = entidadRepositorio.buscarTodas();

    // Algoritmo 1 - Compatibilidad semántica: entidades cuya necesidad
    // (por nombre o descripción) coincide con la subcategoría donada.
    List<EntidadBeneficiaria> porCompatibilidad = todas.stream()
        .filter(e -> sub != null && tieneNecesidadCompatible(e, sub))
        .limit(10)
        .toList();

    // Algoritmo 2 - Prioridad a sub-atendidos: las que menos donaciones tienen asignadas.
    List<EntidadBeneficiaria> porSubatendidos = todas.stream()
        .sorted(Comparator.comparingLong(this::contarAsignadas))
        .limit(10)
        .toList();

    // Coincidencias: aparecen en ambos rankings.
    List<EntidadBeneficiaria> coincidencias = porCompatibilidad.stream()
        .filter(porSubatendidos::contains)
        .toList();

    return CandidatosAsignacionResponseDTO.builder()
        .idDonacion(idDonacion)
        .porCompatibilidad(mapearEntidades(porCompatibilidad))
        .porSubatendidos(mapearEntidades(porSubatendidos))
        .coincidencias(mapearEntidades(coincidencias))
        .build();
  }

  @Override
  public void asignar(UUID idDonacion, AsignacionRequestDTO dto) {
    Donacion donacion = buscarOFallar(idDonacion);

    if (donacion.getEstado() != EstadoDonacion.EN_DEPOSITO) {
      throw new CambioEstadoDonacionIlegalException(donacion.getEstado(), EstadoDonacion.ASIGNACION_REALIZADA);
    }

    EntidadBeneficiaria entidad = entidadRepositorio.obtenerPorId(dto.getIdEntidadBeneficiaria());
    if (entidad == null) {
      throw new EntidadBeneficiariaNoEncontradaException(dto.getIdEntidadBeneficiaria());
    }

    donacion.setIdEntidadBeneficiaria(entidad.getId());
    donacion.getHistorialEstados().add(CambioEstado.builder()
        .estado(EstadoDonacion.ASIGNACION_REALIZADA)
        .justificacion("Asignación confirmada a la entidad " + entidad.getRazonSocial())
        .build());
    donacion.setEstado(EstadoDonacion.ASIGNACION_REALIZADA);

    notificarDonante(donacion);
    notificarEntidad(entidad);
  }

  // ── HELPERS ───────────────────────────────────────────────────────────────

  private Donacion buscarOFallar(UUID id) {
    Donacion donacion = repositorio.obtenerPorId(id);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(id);
    }
    return donacion;
  }

  private List<EntidadBeneficiariaResponseDTO> mapearEntidades(List<EntidadBeneficiaria> entidades) {
    return entidades.stream().map(entidadMapper::toDTO).toList();
  }

  private boolean tieneNecesidadCompatible(EntidadBeneficiaria entidad, String subcategoria) {
    if (entidad.getCampanias() == null) {
      return false;
    }
    String objetivo = subcategoria.toLowerCase();
    return entidad.getCampanias().stream()
        .filter(c -> c.getNecesidades() != null)
        .flatMap(c -> c.getNecesidades().stream())
        .anyMatch(n -> contieneTexto(n.getNombre(), objetivo) || contieneTexto(n.getDescripcion(), objetivo));
  }

  private boolean contieneTexto(String texto, String objetivo) {
    return texto != null && texto.toLowerCase().contains(objetivo);
  }

  private long contarAsignadas(EntidadBeneficiaria entidad) {
    return repositorio.obtenerTodas().stream()
        .filter(d -> entidad.getId().equals(d.getIdEntidadBeneficiaria()))
        .count();
  }

  private void notificarDonante(Donacion donacion) {
    PersonaDonante donante = donanteRepositorio.obtenerPersona(donacion.getIdDonante());
    if (donante == null || donante.getMedioContactoPredeterminado() == null) {
      return;
    }
    MedioDeContacto medio = donante.getMedioContactoPredeterminado();
    notificacionClient.enviarNotificacion(
        medio.getValor(),
        "¡Tu donación fue asignada a una entidad beneficiaria!",
        mapearMedio(medio),
        "ASIGNACION_DONACION_DONANTE");
  }

  private void notificarEntidad(EntidadBeneficiaria entidad) {
    if (entidad.getContactos() == null || entidad.getContactos().isEmpty()) {
      return;
    }
    MedioDeContacto medio = entidad.getContactos().get(0);
    notificacionClient.enviarNotificacion(
        medio.getValor(),
        "Se le asignó una nueva donación según sus necesidades registradas.",
        mapearMedio(medio),
        "ASIGNACION_DONACION_ENTIDAD");
  }

  private String mapearMedio(MedioDeContacto medio) {
    if (medio instanceof Email) return "EMAIL";
    if (medio instanceof Whatsapp) return "WHATSAPP";
    if (medio instanceof Telefono) return "SMS";
    return "EMAIL";
  }
}
