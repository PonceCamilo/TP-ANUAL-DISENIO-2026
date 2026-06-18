/*
package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.importacion.DonanteCsvRowParser;
import ar.utn.donatrack.donaciones.importacion.DonanteFactory;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.repositories.ImportCSVRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CsvImportServiceTest {

    @Mock
    private PersonaDonanteRepositoryInterface repositorio;

    // DonanteCsvRowParser y DonanteFactory son componentes simples sin dependencias,
    // se usan como instancias reales (no hace falta mockearlos)
    private final DonanteCsvRowParser parser = new DonanteCsvRowParser();
    private final DonanteFactory factory = new DonanteFactory();

    private CsvImportService service;

    @BeforeEach
    void setUp() {
        service = new CsvImportService(repositorio, parser, factory);
    }

    @Test
    void archivoValido_creaDonantesYGeneraReporte() throws Exception {
        // obtenerPorMail devuelve null → ambos donantes son nuevos
        when(repositorio.obtenerPorMail(anyString())).thenReturn(null);

        String csvFalso = "TipoPersona,TipoDoc,Documento,NombreORazonSocial,Email,Telefono\n" +
            "HUMANA,DNI,112233,Juan Perez,juan@mail.com,1234\n" +
            "JURIDICA,CUIT,334455,Empresa SA,empresa@mail.com,";

        InputStream inputStream = new ByteArrayInputStream(csvFalso.getBytes(StandardCharsets.UTF_8));

        ImportCSVRepository reporte = service.importar(inputStream);

        assertEquals(2, reporte.totalProcesadas(), "Deberia haber procesado 2 filas");
        assertEquals(2, reporte.totalCreados(), "Deberia haber creado 2 donantes nuevos");
        assertFalse(reporte.tieneErrores(), "No deberia tener errores");
        verify(repositorio, times(2)).guardar(any());
    }

    @Test
    void filaInvalida_noFrenaYRegistraElError() throws Exception {
        // Solo la fila valida (Ana) llega a intentar guardar
        when(repositorio.obtenerPorMail("ana@mail.com")).thenReturn(null);

        String csvFalso = "TipoPersona,TipoDoc,Documento,NombreORazonSocial,Email,Telefono\n" +
            "HUMANA,DNI,112233,Juan Perez,,1234\n" +   // email vacio → error de parseo
            "HUMANA,DNI,998877,Ana Gomez,ana@mail.com,";

        InputStream inputStream = new ByteArrayInputStream(csvFalso.getBytes(StandardCharsets.UTF_8));

        ImportCSVRepository reporte = service.importar(inputStream);

        assertEquals(2, reporte.totalProcesadas(), "Deberia haber intentado procesar ambas filas");
        assertEquals(1, reporte.totalCreados(), "Solo deberia haber creado la fila valida (Ana)");
        assertTrue(reporte.tieneErrores(), "Deberia haber registrado 1 error por la fila rota (Juan)");
        assertEquals(1, reporte.getErrores().size());
        verify(repositorio, times(1)).guardar(any());
    }
}

 */