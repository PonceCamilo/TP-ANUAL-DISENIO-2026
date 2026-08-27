package ar.utn.donatrack.donaciones.models.donante.estado;

import ar.utn.donatrack.donaciones.exceptions.personasExceptions.FaltaJustificacionException;

import java.util.Map;

public class ActivoState extends EstadoDonante {

    public ActivoState() {
        super("ACTIVO", Map.of(
            "INACTIVO",  InactivoState::new,
            "BLOQUEADO", BloqueadoState::new
        ));
    }

    public EstadoDonante transicionarA(String estadoDestino, String justificacion) {
        if ("BLOQUEADO".equals(estadoDestino) && (justificacion == null || justificacion.isBlank())) {
            throw new FaltaJustificacionException("Es obligatorio proveer una justificación para bloquear al donante.");
        }
        return super.transicionarA(estadoDestino, justificacion);
    }

}
