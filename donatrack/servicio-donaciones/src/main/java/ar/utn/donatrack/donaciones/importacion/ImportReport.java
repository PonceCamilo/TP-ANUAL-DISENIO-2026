package ar.utn.donatrack.donaciones.importacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen final del proceso de importación masiva de donantes por CSV.
 * Acumula un ImportResult por cada fila procesada.
 */
public class ImportReport {

    private final List<ImportResult> resultados = new ArrayList<>();

    public void agregar(ImportResult result) {
        resultados.add(result);
    }

    public long totalCreados() {
        return resultados.stream()
                .filter(r -> r.getEstado() == ImportResult.Estado.CREADO)
                .count();
    }

    public long totalActualizados() {
        return resultados.stream()
                .filter(r -> r.getEstado() == ImportResult.Estado.ACTUALIZADO)
                .count();
    }

    public List<ImportResult> getErrores() {
        return resultados.stream()
                .filter(ImportResult::esError)
                .toList();
    }

    public boolean tieneErrores() {
        return resultados.stream().anyMatch(ImportResult::esError);
    }

    public int totalProcesadas() {
        return resultados.size();
    }

    @Override
    public String toString() {
        return "ImportReport{creados=%d, actualizados=%d, errores=%d, total=%d}".formatted(
                totalCreados(), totalActualizados(),
                getErrores().size(), totalProcesadas());
    }
}
