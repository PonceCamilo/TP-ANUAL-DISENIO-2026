package ar.utn.donatrack.donaciones.dtos;

import ar.utn.donatrack.donaciones.dtos.response.ImportacionResumenResponseDTO;
import ar.utn.donatrack.donaciones.importacion.ImportFilaCSV;
import ar.utn.donatrack.donaciones.importacion.ImportReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ImportacionResumenResponseDTO: resumen de la importación por CSV")
class ImportacionResumenResponseDTOTest {

    @Test
    @DisplayName("cuenta las filas correctas y detalla solo los primeros errores")
    void limitaLosErroresDetallados() {
        ImportReport reporte = new ImportReport();
        reporte.agregar(ImportFilaCSV.creado(2, "a@mail.com"));
        reporte.agregar(ImportFilaCSV.actualizado(3, "b@mail.com"));
        for (int linea = 4; linea < 9; linea++) {
            reporte.agregar(ImportFilaCSV.error(linea, "", "fila " + linea + " inválida"));
        }

        ImportacionResumenResponseDTO resumen = ImportacionResumenResponseDTO.desde(reporte, 3);

        assertThat(resumen.getTotalProcesadas()).isEqualTo(7);
        assertThat(resumen.getCreadas()).isEqualTo(1);
        assertThat(resumen.getActualizadas()).isEqualTo(1);
        assertThat(resumen.getConError()).isEqualTo(5);
        assertThat(resumen.getErrores()).extracting(ImportacionResumenResponseDTO.ErrorDeFila::numeroLinea)
                .containsExactly(4, 5, 6);
        assertThat(resumen.getErroresOmitidos()).isEqualTo(2);
    }

    @Test
    void sinErroresNoOmiteNada() {
        ImportReport reporte = new ImportReport();
        reporte.agregar(ImportFilaCSV.creado(2, "a@mail.com"));

        ImportacionResumenResponseDTO resumen = ImportacionResumenResponseDTO.desde(reporte, 100);

        assertThat(resumen.getErrores()).isEmpty();
        assertThat(resumen.getErroresOmitidos()).isZero();
    }
}
