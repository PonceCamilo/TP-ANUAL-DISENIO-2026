package ar.utn.donatrack.autenticacion.dtos.response;

import java.util.List;

/** Qué pasó con cada persona de la tanda: cuenta creada, ya existía o rechazada (con el motivo). */
public record ResultadoImportacion(int creadas, int existentes, List<Rechazo> rechazadas) {
    public record Rechazo(String email, String motivo) {}
}
