package ar.utn.donatrack.incentivos.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** Insignia otorgada al donante al completar una misión. */
public class Insignia {

    private final UUID id;
    private final UUID donanteId;
    private final Mision misionOrigen;    // qué misión la generó
    private final LocalDateTime otorgadaEn;
    private boolean visible;              // el donante elige si mostrarla en su perfil

    public Insignia(UUID donanteId, Mision misionOrigen) {
        this.id           = UUID.randomUUID();
        this.donanteId    = donanteId;
        this.misionOrigen = misionOrigen;
        this.otorgadaEn   = LocalDateTime.now();
        this.visible      = true; // visible por defecto
    }

    public void setVisible(boolean visible) { this.visible = visible; }

    public UUID getId()                  { return id; }
    public UUID getDonanteId()           { return donanteId; }
    public Mision getMisionOrigen()      { return misionOrigen; }
    public LocalDateTime getOtorgadaEn() { return otorgadaEn; }
    public boolean isVisible()           { return visible; }
}
