package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.DonacionServiceInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.validations.DonacionesValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonacionService implements DonacionServiceInterface {

  private final DonacionesRepositoryInterface repositorio;
  private final DonacionesValidator validador;
  private final DonacionMapper mapper;
  private final EntidadesBeneficiariasRepositoryInterface entidadesRepository;
  private final PersonaDonanteRepositoryInterface donanteRepository;
  private final NotificacionClient notificacionClient;

  public List<DonacionResponseDTO> obtenerDonaciones(EstadoDonacion estado) {
    if (estado != null) {
      return mapper.toDTOList(repositorio.obtenerPorEstado(estado));
    }
    return mapper.toDTOList(repositorio.obtenerTodas());
  }

  public DonacionResponseDTO obtenerPorId(UUID id) {
    Donacion donacion = repositorio.obtenerPorId(id);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(id);
    }
    return mapper.toDTO(donacion);
  }

  public void cambiarEstado(UUID id, CambioEstadoRequestDTO dto) {
    Donacion donacion = repositorio.obtenerPorId(id);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(id);
    }

    validador.validarTransicion(donacion.getEstado(), dto.getEstado(), dto.getJustificacion());

    donacion.getHistorialEstados().add(CambioEstado.builder()
            .estado(dto.getEstado())
            .justificacion(dto.getJustificacion())
            .build());
    donacion.setEstado(dto.getEstado());
  }

  public void modificarBien(UUID id, BienRequestDTO dto) {
    Donacion donacion = repositorio.obtenerPorId(id);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(id);
    }
    donacion.getBienes().set(0, mapper.toBien(dto));
  }

  public void eliminar(UUID id) {
    if (repositorio.obtenerPorId(id) == null) {
      throw new DonacionNoEncontradaException(id);
    }
    repositorio.eliminar(id);
  }

  public void asignarEntidad(UUID idDonacion, UUID idEntidad) {
    Donacion donacion = repositorio.obtenerPorId(idDonacion);
    if (donacion == null) {
      throw new DonacionNoEncontradaException(idDonacion);
    }

    EntidadBeneficiaria entidad = entidadesRepository.obtenerPorId(idEntidad);
    if (entidad == null) {
      throw new EntidadBeneficiariaNoEncontradaException(idEntidad);
    }

    validador.validarTransicion(donacion.getEstado(), EstadoDonacion.ASIGNACION_REALIZADA, null);

    donacion.setIdEntidadAsignada(idEntidad);
    donacion.setFechaAsignacion(LocalDate.now());
    donacion.getHistorialEstados().add(CambioEstado.builder()
            .estado(EstadoDonacion.ASIGNACION_REALIZADA)
            .justificacion("Asignada a " + entidad.getRazonSocial())
            .build());
    donacion.setEstado(EstadoDonacion.ASIGNACION_REALIZADA);

    notificarAsignacion(donacion, entidad);
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

    PersonaDonante donante = donanteRepository.obtenerPersona(donacion.getIdDonante());
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
    return contactos.stream()
            .filter(c -> c instanceof Email)
            .map(MedioDeContacto::getValor)
            .findFirst()
            .orElse(null);
  }
}