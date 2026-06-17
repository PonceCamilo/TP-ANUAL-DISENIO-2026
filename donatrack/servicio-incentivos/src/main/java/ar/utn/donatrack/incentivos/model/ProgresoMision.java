package ar.utn.donatrack.incentivos.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registra el avance de un donante en una misión específica.
 * Un donante tiene un ProgresoMision por cada misión disponible.
 */
public class ProgresoMision {

    private final UUID id;
    private final UUID donanteId;
    private final Mision mision;
    private int progresoActual;
    private boolean completada;
    private LocalDateTime fechaCompletada;

    public ProgresoMision(UUID donanteId, Mision mision) {
        this.id             = UUID.randomUUID();
        this.donanteId      = donanteId;
        this.mision         = mision;
        this.progresoActual = 0;
        this.completada     = false;
    }

    /**
     * Incrementa el progreso. Si se alcanza el objetivo, marca la misión como completada.
     * @return true si se acaba de completar con este incremento
     */
    public boolean incrementarProgreso(int cantidad) {
        if (completada) return false;
        progresoActual += cantidad;
        if (progresoActual >= mision.getObjetivo()) {
            completada       = true;
            fechaCompletada  = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /** Para misiones tipo RACHA: resetea el progreso si no se cumplió la condición. */
    public void resetearProgreso() {
        if (!completada) {
            progresoActual = 0;
        }
    }

    public UUID getId()                       { return id; }
    public UUID getDonanteId()                { return donanteId; }
    public Mision getMision()                 { return mision; }
    public int getProgresoActual()            { return progresoActual; }
    public boolean isCompletada()             { return completada; }
    public LocalDateTime getFechaCompletada() { return fechaCompletada; }
    public int getDistanciaRestante() {
        return Math.max(0, mision.getObjetivo() - progresoActual);
    }
}
