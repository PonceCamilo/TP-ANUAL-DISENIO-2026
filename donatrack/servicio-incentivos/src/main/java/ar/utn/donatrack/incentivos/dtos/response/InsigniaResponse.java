package ar.utn.donatrack.incentivos.dtos.response;

import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;

import java.time.LocalDate;
import java.util.UUID;

public record InsigniaResponse(
        UUID id,
        String nombre,
        String imagen,
        LocalDate otorgadaEn,
        boolean visible
) {
    public static InsigniaResponse desde(InsigniaObtenida i) {
        return new InsigniaResponse(
                i.getId(),
                i.getInsignia().getNombre(),
                i.getInsignia().getImagen(),
                i.getFechaObtencion(),
                i.isVisibilidad()
        );
    }
}
