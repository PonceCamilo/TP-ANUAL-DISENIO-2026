package ar.utn.donatrack.incentivos.dtos.response;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.misiones.Mision;

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
        LocalDateTime fechaCompletada
) {
    public static MisionResponse desde(Mision m, Donante d) {
        return new MisionResponse(
                m.getId(),
                m.getNombre(),
                m.getDescripcion(),
                m.getClass().getSimpleName(), 
                m.getCategoriaRequerida().getClass().getSimpleName(),
                m.getObjetivo(),
                m.progresoActual(d),
                m.restante(d),
                m.estaCompletada(d),
                null
        );
    }
}
