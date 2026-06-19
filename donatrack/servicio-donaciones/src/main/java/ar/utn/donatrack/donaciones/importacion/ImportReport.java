package ar.utn.donatrack.donaciones.importacion;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen final del proceso de importación masiva de donantes por CSV.
 * Acumula un ImportFilaCSV por cada fila procesada.
 *
 * Renombrado de ImportCSVRepository a ImportReport: no es un repositorio
 * (no persiste datos), es un objeto de resultado de la operación de importación.
 */

@Getter
public class ImportReport {

    private final List<ImportFilaCSV> resultados = new ArrayList<>();

    public void agregar(ImportFilaCSV result) {
        resultados.add(result);
    }

    public long totalCreados() {
        return resultados.stream()
            .filter(r -> r.getEstado() == EstadoImport.CREADO)
            .count();
    }

    public long totalActualizados() {
        return resultados.stream()
            .filter(r -> r.getEstado() == EstadoImport.ACTUALIZADO)
            .count();
    }

    public List<ImportFilaCSV> getErrores() {
        return resultados.stream()
            .filter(ImportFilaCSV::esError)
            .toList();
    }

    public boolean tieneErrores() {
        return resultados.stream().anyMatch(ImportFilaCSV::esError);
    }

    public int totalProcesadas() {
        return resultados.size();
    }

    @Override
    public String toString() {
        return "ImportReport{creados=%d, actualizados=%d, errores=%d, total=%d}".formatted(
            totalCreados(), totalActualizados(), getErrores().size(), totalProcesadas()
        );
    }
}
