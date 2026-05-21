package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.importacion.EstadoImport;
import ar.utn.donatrack.donaciones.importacion.ImportFilaCSV;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen final del proceso de importación masiva de donantes por CSV.
 * Acumula un ImportResult por cada fila procesada.
 */

@Getter
@Setter
public class ImportCSVRepository {

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
                totalCreados(), totalActualizados(),
                getErrores().size(), totalProcesadas());
    }
}
