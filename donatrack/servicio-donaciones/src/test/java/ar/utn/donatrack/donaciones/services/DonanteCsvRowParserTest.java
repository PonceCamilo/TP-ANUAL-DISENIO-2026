package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.excepcion.CsvFormatoInvalidoException;
import ar.utn.donatrack.donaciones.importacion.*;
import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DonanteCsvRowParserTest {

    // Instanciamos el objeto real que vamos a testear
    private final DonanteCsvRowParser parser = new DonanteCsvRowParser();

    @Test
    void filaValida_retornaDtoCorrectamente() {
        // Simulamos las columnas que leería de una fila del CSV
        String[] columnas = {"HUMANA", "DNI", "12345678", "Juan Perez", "juan@mail.com", "1122334455"};
        
        DonanteImportDto dto = parser.parsear(columnas, 2);

        // Verificamos que armó el objeto con los datos correctos
        assertEquals("HUMANA", dto.tipoPersona());
        assertEquals("12345678", dto.documento());
        assertEquals("juan@mail.com", dto.email());
    }

    @Test
    void filaSinEmail_lanzaExcepcion() {
        // Le pasamos una fila sin email
        String[] columnas = {"JURIDICA", "CUIT", "30-11111111-2", "Empresa SA", "", "1122334455"};

        // Verificamos que al intentar parsearlo salta la excepción correcta
        CsvFormatoInvalidoException excepcion = assertThrows(
                CsvFormatoInvalidoException.class, 
                () -> parser.parsear(columnas, 3)
        );
        
        // Comprobamos que el mensaje de error sea descriptivo
        assertTrue(excepcion.getMessage().contains("el email es obligatorio"));
    }
}