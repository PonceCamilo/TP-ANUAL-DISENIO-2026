package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.importacion.ImportFilaCSV;
import ar.utn.donatrack.donaciones.importacion.ImportReport;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Respuesta de POST /donantes/import: un resumen de la importación y el detalle
 * de las filas con error.
 *
 * Antes se devolvía el ImportReport completo, con una entrada por cada fila
 * procesada: con 5.000 filas la respuesta pesaba 470 KB y casi todo era
 * «creada» o «actualizada». Ahora las filas correctas solo se cuentan, y de los
 * errores se detallan los primeros (donaciones.importacion.max-errores-en-respuesta);
 * los demás quedan contados en erroresOmitidos.
 */
@Getter
@Builder
public class ImportacionResumenResponseDTO {

    private final int totalProcesadas;
    private final long creadas;
    private final long actualizadas;
    private final int conError;
    private final List<ErrorDeFila> errores;
    /** Errores que no se detallan en `errores` porque superan el máximo de la respuesta. */
    private final int erroresOmitidos;

    /** Una fila que no se pudo importar, con la línea del archivo para poder corregirla. */
    public record ErrorDeFila(int numeroLinea, String email, String detalle) {
    }

    public static ImportacionResumenResponseDTO desde(ImportReport reporte, int maximoErrores) {
        List<ImportFilaCSV> errores = reporte.getErrores();
        return ImportacionResumenResponseDTO.builder()
                .totalProcesadas(reporte.totalProcesadas())
                .creadas(reporte.totalCreados())
                .actualizadas(reporte.totalActualizados())
                .conError(errores.size())
                .errores(errores.stream()
                        .limit(maximoErrores)
                        .map(fila -> new ErrorDeFila(fila.getNumeroLinea(), fila.getEmail(), fila.getDetalle()))
                        .toList())
                .erroresOmitidos(Math.max(0, errores.size() - maximoErrores))
                .build();
    }
}
