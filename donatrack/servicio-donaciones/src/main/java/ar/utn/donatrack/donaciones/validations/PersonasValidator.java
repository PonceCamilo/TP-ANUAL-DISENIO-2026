package ar.utn.donatrack.donaciones.validations;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.EstadoNoExistenteException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.MedioContactoInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMimsoEstadoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.repositories.PersonaDonanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PersonasValidator {

  private final PersonaDonanteRepositoryInterface repositorio;

  public void validarEmailNoDuplicado(String email) {
    if (repositorio.existePorEmail(email)) {
      throw new EmailYaRegistradoException();
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

  public void validarCambioEstado(EstadoDonante actual, EstadoDonante nuevo) {
    if (actual == nuevo) {
      throw new PersonaConMismoEstadoException(nuevo);
    }
  }
}