package ar.utn.donatrack.donaciones.importacion;

import lombok.Builder;
import lombok.Getter;

/**
 * Resultado del procesamiento de una sola fila del CSV.
 * Se acumula en ImportReport al finalizar la importación.
 */

@Builder
@Getter
public class ImportFilaCSV {

    private final int numeroLinea;
    private final EstadoImport estado;
    private final String email;
    private final String detalle; // null si no hay error

    // ─── Métodos de fábrica ───────────────────────────────────────────────────

    public static ImportFilaCSV creado(int linea, String email) {
        return new ImportFilaCSV(linea, EstadoImport.CREADO, email, null);
    }

    public static ImportFilaCSV actualizado(int linea, String email) {
        return new ImportFilaCSV(linea, EstadoImport.ACTUALIZADO, email, null);
    }

    public static ImportFilaCSV error(int linea, String email, String motivo) {
        return new ImportFilaCSV(linea, EstadoImport.ERROR, email, motivo);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public boolean esError() { return estado == EstadoImport.ERROR; }

    @Override
    public String toString() {
        return "[%s] línea %d - %s%s".formatted(
                estado, numeroLinea, email,
                detalle != null ? ": " + detalle : "");
    }
}
