package ar.utn.donatrack.incentivos.dtos.response;

import java.util.UUID;

/**
 * Un puesto del ranking. Entrega 6: con el nombre visible de la persona (nombre
 * e inicial del apellido, o razón social), que manda Donaciones; null si todavía
 * no llegó ningún aviso con el nombre.
 */
public record PuestoRankingResponse(int puesto, UUID donanteId, String nombreVisible, int misionesCompletadas) {

    public PuestoRankingResponse(int puesto, UUID donanteId, int misionesCompletadas) {
        this(puesto, donanteId, null, misionesCompletadas);
    }

    public PuestoRankingResponse conNombre(String nombre) {
        return new PuestoRankingResponse(puesto, donanteId, nombre, misionesCompletadas);
    }
}
