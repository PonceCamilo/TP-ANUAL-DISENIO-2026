package ar.utn.donatrack.donaciones.validations.personas;

import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.MedioContactoInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
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

  private final RepositorioPersonasDonantes repositorio;

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

  /** Recupera la persona donante o lanza la excepción de dominio si no existe (404). */
  public PersonaDonante validarYObtenerPersona(UUID id) {
    return repositorio.buscarPorId(id).orElseThrow(() -> new PersonaDonanteNoEncontradaException(id));
  }

  /**
   * Recupera una persona jurídica: 404 si no existe y 400 si es humana. Una sola
   * consulta: antes se verificaba la existencia y el tipo por separado, y después
   * el repositorio la volvía a leer.
   */
  public PersonaJuridica validarYObtenerJuridica(UUID id) {
    if (!(validarYObtenerPersona(id) instanceof PersonaJuridica juridica)) {
      throw new TipoPersonaIlegalException(id);
    }
    return juridica;
  }

  public void validarMedioContacto(MedioDeContacto medio) {
    if (medio == null || medio.getValor() == null || medio.getValor().isBlank()) {
      throw new MedioContactoInvalidoException(medio == null ? null : medio.getValor());
    }
  }
}
