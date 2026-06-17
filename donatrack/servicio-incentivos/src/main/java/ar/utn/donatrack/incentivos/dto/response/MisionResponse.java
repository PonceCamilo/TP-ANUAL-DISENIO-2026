package ar.utn.donatrack.incentivos.dto.response;

import ar.utn.donatrack.incentivos.model.ProgresoMision;

import java.time.LocalDateTime;
import java.util.UUID;

public record MisionResponse(
        UUID misionId,
        String nombre,
        String descripcion,
        String tipo,
        String categoriaRequerida,
        int objetivo,
        int progresoActual,
        int distanciaRestante,
        boolean completada,
        LocalDateTime fechaCompletada,
        int orden
) {
    public static MisionResponse desde(ProgresoMision p) {
        return new MisionResponse(
                p.getMision().getId(),
                p.getMision().getNombre(),
                p.getMision().getDescripcion(),
                p.getMision().getTipo().name(),
                p.getMision().getCategoriaRequerida().name(),
                p.getMision().getObjetivo(),
                p.getProgresoActual(),
                p.getDistanciaRestante(),
                p.isCompletada(),
                p.getFechaCompletada(),
                p.getMision().getOrden()
        );
    }
}
