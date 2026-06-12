package ar.utn.donatrack.donaciones.validations;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.CambioEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import org.springframework.stereotype.Component;

@Component
public class DonacionesValidator {

  public void validarTransicion(EstadoDonacion estadoActual, EstadoDonacion estadoNuevo, String justificacion) {

    if(justificacion == null || justificacion.isBlank()) {
      throw new FaltaJustificacionException();
    }

    boolean valida = switch (estadoActual) {
      case ENTREGA_FALLIDA -> estadoNuevo == EstadoDonacion.EN_DEPOSITO;
      case EN_DEPOSITO -> estadoNuevo == EstadoDonacion.ASIGNACION_REALIZADA || estadoNuevo == EstadoDonacion.VENCIDA;
      case ASIGNACION_REALIZADA -> estadoNuevo == EstadoDonacion.LISTA_PARA_ENTREGAR;
      case LISTA_PARA_ENTREGAR -> estadoNuevo == EstadoDonacion.EN_TRASLADO;
      case EN_TRASLADO -> estadoNuevo == EstadoDonacion.ENTREGADA || estadoNuevo == EstadoDonacion.ENTREGA_FALLIDA;
      default -> false;
    };
    if (!valida) {
      throw new CambioEstadoIlegalException(estadoActual, estadoNuevo);
    }
  }
}