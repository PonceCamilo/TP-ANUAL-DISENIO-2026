package ar.utn.donatrack.incentivos.dtos.response;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.misiones.Mision;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MisionResponse(
        Long misionId,
        String nombre,
        String descripcion,
        String tipo,
        String categoriaRequerida,
        int objetivo,
        int progresoActual,
        int distanciaRestante,
        boolean activa,
        boolean completada,
        LocalDateTime fechaCompletada
) {
    public static MisionResponse desde(Mision m, Donante d) {
        boolean activa = m.equals(d.getProgresoMision().getMisionActual());
        boolean completada = !activa && d.completoMision(m);
        int progreso = activa ? d.getProgresoMision().progresoActual(LocalDate.now())
                : completada ? m.getObjetivo() : 0;
        return new MisionResponse(
                m.getId(),
                m.getNombre(),
                m.getDescripcion(),
                m.getClass().getSimpleName(), 
                m.getCategoriaRequerida().getClass().getSimpleName(),
                m.getObjetivo(),
                progreso,
                Math.max(0, m.getObjetivo() - progreso),
                activa,
                completada,
                null
        );
    }
}
