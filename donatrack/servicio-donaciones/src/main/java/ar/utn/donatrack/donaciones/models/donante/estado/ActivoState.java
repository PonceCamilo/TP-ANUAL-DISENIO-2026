package ar.utn.donatrack.donaciones.models.donante.estado;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;

import java.util.Map;

import static ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante.ACTIVO;
import static ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante.BLOQUEADO;
import static ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante.INACTIVO;

public class ActivoState extends EstadoDonante {

    public ActivoState() {
        super(ACTIVO, Map.of(
                INACTIVO, InactivoState::new,
                BLOQUEADO, BloqueadoState::new));
    }

    @Override
    protected void validarTransicion(String estadoDestino, String justificacion) {
        super.validarTransicion(estadoDestino, justificacion);
        if (BLOQUEADO.equals(estadoDestino) && sinJustificacion(justificacion)) {
            throw new FaltaJustificacionException("Es obligatorio proveer una justificación para bloquear al donante.");
        }
    }
}
