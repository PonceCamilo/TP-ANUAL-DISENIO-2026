package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.importacion.*;
import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.NotificacionServiceInterface;
import ar.utn.donatrack.donaciones.repositories.ImportCSVRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream; // permite escribir un String y hacerle creer a Java que es un archivo que está leyendo.
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CsvImportServiceTest {

    // 1. Armamos dependencias "falsas" (Stubs) para no usar Mocks
    //  implementamos lo mínimo y necesario que usa el CsvImportService.
    private final DonanteRepositoryInterface repoFalso = new DonanteRepositoryInterface() {
        @Override public Optional<PersonaDonante> findByEmail(String email) { 
            return Optional.empty(); // Simulamos que el usuario nunca existe
        }
        @Override public void save(PersonaDonante donante) {
            // No hacemos nada, solo simulamos que se guardó
        }
    };
    
    private final NotificacionServiceInterface notiFalsa = new NotificacionServiceInterface() {
        @Override public void enviarCredencialesNuevoUsuario(PersonaDonante donante) {
            // No hacemos nada, evitamos mandar un mail real
        }
    };

    private final DonanteCsvRowParser parser = new DonanteCsvRowParser();
    private final DonanteFactory factory = new DonanteFactory();

    // 2. Instanciamos el servicio pasándole nuestras dependencias controladas
    private final CsvImportService service = new CsvImportService(repoFalso, parser, factory, notiFalsa);

    @Test
    void archivoValido_creaDonantesYGeneraReporte() throws Exception {
        // Simulamos el contenido de un archivo CSV con saltos de línea (\n)
        String csvFalso = "TipoPersona,TipoDoc,Documento,NombreORazonSocial,Email,Telefono\n" +
                          "HUMANA,DNI,112233,Juan Perez,juan@mail.com,1234\n" +
                          "JURIDICA,CUIT,334455,Empresa SA,empresa@mail.com,";
        
        // Convertimos el String en un formato que el servicio pueda leer (InputStream)
        InputStream inputStream = new ByteArrayInputStream(csvFalso.getBytes(StandardCharsets.UTF_8));

        // Ejecutamos el método a testear
        ImportCSVRepository reporte = service.importar(inputStream);

        // Verificamos los resultados lógicos
        assertEquals(2, reporte.totalProcesadas(), "Debería haber procesado 2 filas");
        assertEquals(2, reporte.totalCreados(), "Debería haber creado 2 donantes nuevos");
        assertFalse(reporte.tieneErrores(), "No debería tener errores");
    }

    @Test
    void FilaInvalida_noFrenaYRegistraElError() throws Exception {
        // La primera fila está rota (le falta el email), la segunda está perfecta
        String csvFalso = "TipoPersona,TipoDoc,Documento,NombreORazonSocial,Email,Telefono\n" +
                          "HUMANA,DNI,112233,Juan Perez,,1234\n" +
                          "HUMANA,DNI,998877,Ana Gomez,ana@mail.com,";
        
        InputStream inputStream = new ByteArrayInputStream(csvFalso.getBytes(StandardCharsets.UTF_8));

        ImportCSVRepository reporte = service.importar(inputStream);

        // Verificamos la tolerancia a fallos
        assertEquals(2, reporte.totalProcesadas(), "Debería haber intentado procesar ambas filas");
        assertEquals(1, reporte.totalCreados(), "Solo debería haber creado la fila válida (Ana)");
        assertTrue(reporte.tieneErrores(), "Debería haber registrado 1 error por la fila rota (Juan)");
        assertEquals(1, reporte.getErrores().size());
    }
}