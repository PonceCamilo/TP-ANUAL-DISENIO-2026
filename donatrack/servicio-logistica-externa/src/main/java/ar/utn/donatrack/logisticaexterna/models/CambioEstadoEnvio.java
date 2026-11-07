package ar.utn.donatrack.logisticaexterna.models;

import java.time.LocalDateTime;

public record CambioEstadoEnvio(EstadoEnvio estado, LocalDateTime fecha, String detalle) {
}
