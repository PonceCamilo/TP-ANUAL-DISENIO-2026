package ar.utn.donatrack.incentivos.dto.response;

import ar.utn.donatrack.incentivos.model.Insignia;

import java.time.LocalDateTime;
import java.util.UUID;

public record InsigniaResponse(
        UUID id,
        UUID donanteId,
        String misionNombre,
        LocalDateTime otorgadaEn,
        boolean visible
) {
    public static InsigniaResponse desde(Insignia i) {
        return new InsigniaResponse(
                i.getId(),
                i.getDonanteId(),
                i.getMisionOrigen().getNombre(),
                i.getOtorgadaEn(),
                i.isVisible()
        );
    }
}
