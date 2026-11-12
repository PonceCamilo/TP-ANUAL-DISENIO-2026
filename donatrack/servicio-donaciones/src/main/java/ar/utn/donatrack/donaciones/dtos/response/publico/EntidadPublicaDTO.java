package ar.utn.donatrack.donaciones.dtos.response.publico;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * Una entidad beneficiaria para que las personas donantes la conozcan (Entrega 6,
 * requisito 6): dónde está y qué necesita hoy. Sin datos de contacto.
 */
@Getter
@Builder
public class EntidadPublicaDTO {
    private final UbicacionPublicaDTO ubicacion;
    /** Necesidades de campañas vigentes que todavía no se completaron. */
    private final List<NecesidadAbierta> necesidadesAbiertas;

    @Getter
    @Builder
    public static class NecesidadAbierta {
        private final String nombre;
        private final String descripcion;
        private final String subcategoria;
        private final int cantidadObjetivo;
        private final int cantidadRecibida;
        private final String campania;
        private final LocalDate finDeCampania;
    }
}
