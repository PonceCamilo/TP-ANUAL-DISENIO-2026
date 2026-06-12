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
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PersonasValidator {

  private final PersonaDonanteRepositoryInterface repositorio = new PersonaDonanteRepository();

  public void validarExistenciaMail(String mail){

    if(mail == null || mail.isBlank()) {
      throw new EmailInvalidoException();
    }

    if (repositorio.obtenerPersona(null, mail) != null) {
      throw new EmailYaRegistradoException();
    }
  }

  public void validarExistenciaPersona(UUID idPersona, String mail){
    if(repositorio.obtenerPersona(idPersona, mail) == null){
      throw new PersonaDonanteNoEncontradaException(idPersona);
    }
  }

  public void validarTipoPersona(UUID idPersona){
    if(!(repositorio.obtenerPersona(idPersona, null) instanceof PersonaJuridica)){
      throw new TipoPersonaIlegalException();
    }
  }

  public void validarCambioEstado(EstadoDonante estadoActual, EstadoDonante nuevoEstado){
    if(estadoActual == nuevoEstado){
      throw new PersonaConMimsoEstadoException(nuevoEstado);
    }
  }

  public void validarMedioContacto(MedioDeContacto contacto) {
    if(contacto == null || contacto.getValor() == null || contacto.getValor().isBlank()) {
      throw new MedioContactoInvalidoException();
    }
  }

  public void validarExistenciaEstado(EstadoDonante estado) {
    if(estado == null || (!estado.equals(EstadoDonante.ACTIVO) && !estado.equals(EstadoDonante.INACTIVO))) {
      throw new EstadoNoExistenteException(estado);
    }
  }
}
