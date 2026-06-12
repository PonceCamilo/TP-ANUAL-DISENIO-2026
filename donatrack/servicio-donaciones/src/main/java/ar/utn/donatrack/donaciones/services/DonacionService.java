package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.EstadoDonacion;
import ar.utn.donatrack.donaciones.validations.DonacionesValidator;

public class DonacionService {

  private DonacionesValidator validadorDonaciones;

  public void cambiarEstado(Donacion donacion, EstadoDonacion estadoNuevo, String justificacion) {

    validadorDonaciones.validarTransicion(donacion.getEstado(), estadoNuevo, justificacion);

    donacion.getHistorialEstados().add(CambioEstado.builder()
        .estado(estadoNuevo)
        .justificacion(justificacion)
        .build());
    donacion.setEstado(estadoNuevo);
  }
}