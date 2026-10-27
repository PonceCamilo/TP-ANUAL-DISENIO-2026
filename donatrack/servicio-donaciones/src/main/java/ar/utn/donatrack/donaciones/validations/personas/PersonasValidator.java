package ar.utn.donatrack.donaciones.validations.personas;

import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.MedioContactoInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PersonasValidator {

  private final PersonaDonanteRepositoryInterface repositorio;

  /**
   * La parte local admite letras y dígitos Unicode (\p{L}, \p{N}): los emails con
   * tilde, como los del CSV de prueba del enunciado, son válidos. Antes solo se
   * admitían letras sin tilde, así que un donante importado por CSV no podía
   * registrarse por la API con su mismo email. @Email de Bean Validation ya los
   * aceptaba; esta expresión era la única que los rechazaba.
   */
  private static final Pattern EMAIL = Pattern.compile("^[\\p{L}\\p{N}+_.-]+@(.+)$");

  public void validarEmail(String email) {
    String normalizado = NormalizadorEmail.normalizar(email);
    if (normalizado == null || !EMAIL.matcher(normalizado).matches()) {
      throw new EmailInvalidoException(email);
    }
    if (repositorio.existePorEmail(normalizado)) {
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
}
