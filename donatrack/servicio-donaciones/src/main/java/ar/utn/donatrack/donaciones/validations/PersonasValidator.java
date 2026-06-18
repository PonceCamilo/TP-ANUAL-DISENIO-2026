package ar.utn.donatrack.donaciones.validations;

import ar.utn.donatrack.donaciones.exceptions.personasExceptions.CambioEstadoPersonaIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.MedioContactoInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PersonasValidator {

  private final PersonaDonanteRepositoryInterface repositorio;

  // Regex básica para validar emails
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

  public void validarEsPersonaJuridica(UUID id) {
    if (!(repositorio.obtenerPersona(id) instanceof PersonaJuridica)) {
      throw new TipoPersonaIlegalException();
    }
  }

  public void validarMedioContacto(MedioDeContacto medio) {
    if (medio == null || medio.getValor() == null || medio.getValor().isBlank()) {
      throw new MedioContactoInvalidoException();
    }
    // Aquí podrías agregar más validaciones dependiendo del tipo de contacto (ej. si es teléfono que solo tenga números)
  }

  public void validarCambioEstado(EstadoDonante actual, EstadoDonante nuevo, String justificacion) {
    // 1. No se permite "cambiar" al mismo estado actual.
    if (actual == nuevo) {
      throw new PersonaConMismoEstadoException(nuevo);
    }

    // 2. Justificación obligatoria para bloquear a un donante.
    if (nuevo == EstadoDonante.BLOQUEADO && (justificacion == null || justificacion.isBlank())) {
      throw new FaltaJustificacionException("Es obligatorio proveer una justificación para bloquear al donante.");
    }

    // 3. Matriz explícita de transiciones permitidas.
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