package ar.utn.donatrack.donaciones.validations.personas;

import ar.utn.donatrack.donaciones.exceptions.personasExceptions.CambioEstadoPersonaIlegalException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.MedioContactoInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PersonasValidator {

  private final PersonaDonanteRepositoryInterface repositorio;

  private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";

  public void validarEmail(String email) {
    if (email == null || !Pattern.compile(EMAIL_REGEX).matcher(email).matches()) {
      throw new EmailInvalidoException(email);
    }
    if (repositorio.existePorEmail(email)) {
      throw new EmailYaRegistradoException(email);
    }
  }

  public void validarExistenciaPersona(UUID id) {
    if (!repositorio.existePorId(id)) {
      throw new PersonaDonanteNoEncontradaException(id);
    }
  }

  /** Recupera la persona donante o lanza la excepción de dominio si no existe. */
  public PersonaDonante validarYObtenerPersona(UUID id) {
    PersonaDonante persona = repositorio.obtenerPersona(id);
    if (persona == null) {
      throw new PersonaDonanteNoEncontradaException(id);
    }
    return persona;
  }

  public void validarEsPersonaJuridica(UUID id) {
    if (!(repositorio.obtenerPersona(id) instanceof PersonaJuridica)) {
      throw new TipoPersonaIlegalException(id);
    }
  }

  public void validarMedioContacto(MedioDeContacto medio) {
    if (medio == null || medio.getValor() == null || medio.getValor().isBlank()) {
      throw new MedioContactoInvalidoException(medio == null ? null : medio.getValor());
    }
  }

  public void validarCambioEstado(EstadoDonante actual, EstadoDonante nuevo, String justificacion) {
    if (actual == nuevo) {
      throw new PersonaConMismoEstadoException(nuevo);
    }

    if (nuevo == EstadoDonante.BLOQUEADO && (justificacion == null || justificacion.isBlank())) {
      throw new FaltaJustificacionException("Es obligatorio proveer una justificación para bloquear al donante.");
    }

    boolean valida = switch (actual) {
      case ACTIVO -> nuevo == EstadoDonante.INACTIVO || nuevo == EstadoDonante.BLOQUEADO;
      case INACTIVO -> nuevo == EstadoDonante.ACTIVO;
      case BLOQUEADO -> nuevo == EstadoDonante.ACTIVO;
    };

    if (!valida) {
      throw new CambioEstadoPersonaIlegalException(actual, nuevo);
    }
  }
}
