package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.exceptions.csvExcepctions.CsvFormatoException;
import ar.utn.donatrack.donaciones.importacion.DonanteCsvRowParser;
import ar.utn.donatrack.donaciones.importacion.DonanteFactory;
import ar.utn.donatrack.donaciones.importacion.ImportFilaCSV;
import ar.utn.donatrack.donaciones.importacion.ImportReport;
import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestador del proceso de importación masiva de personas donantes por CSV.
 *
 * Responsabilidades:
 *  - Leer el archivo línea a línea en lotes de BATCH_SIZE registros.
 *  - Delegar el parseo a DonanteCsvRowParser.
 *  - Decidir si crear o actualizar usando el email como clave de idempotencia.
 *  - Delegar la construcción de objetos de dominio a DonanteFactory.
 *  - Acumular resultados en un ImportReport sin detener el proceso ante errores parciales.
 */

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final int BATCH_SIZE = 500;

    private final PersonaDonanteRepositoryInterface donanteRepository;
    private final DonanteCsvRowParser parser;
    private final DonanteFactory factory;

    public ImportReport importar(InputStream csvStream) throws IOException {
        ImportReport report = new ImportReport();

        try (CSVReader csvReader = new CSVReaderBuilder(
            new InputStreamReader(csvStream, StandardCharsets.UTF_8))
            .withSkipLines(1)
            .build()) {

            List<DonanteImportDto> lote = new ArrayList<>(BATCH_SIZE);
            String[] columnas;
            int numeroLinea = 2;

            while ((columnas = csvReader.readNext()) != null) {
                if (columnas.length == 0) { numeroLinea++; continue; }

                try {
                    DonanteImportDto dto = parser.parsear(columnas, numeroLinea);
                    lote.add(dto);
                } catch (CsvFormatoException e) {
                    report.agregar(ImportFilaCSV.error(numeroLinea, "", e.getMessage()));
                }

                if (lote.size() >= BATCH_SIZE) {
                    procesarLote(lote, report, numeroLinea - lote.size() + 1);
                    lote.clear();
                }
                numeroLinea++;
            }

            if (!lote.isEmpty()) {
                procesarLote(lote, report, numeroLinea - lote.size());
            }

        } catch (CsvValidationException e) {
            throw new IOException("Error al leer el archivo CSV: " + e.getMessage(), e);
        }

        return report;
    }

    private void procesarLote(List<DonanteImportDto> lote, ImportReport report, int lineaInicio) {
        int lineaActual = lineaInicio;
        for (DonanteImportDto dto : lote) {
            report.agregar(procesarFila(dto, lineaActual));
            lineaActual++;
        }
    }

    private ImportFilaCSV procesarFila(DonanteImportDto dto, int linea) {
        try {
            PersonaDonante existente = donanteRepository.obtenerPorEmail(dto.email());

            if (existente != null) {
                // El email es la clave de idempotencia: nunca se modifica.
                // Se actualizan el resto de los datos identificatorios del CSV.
                existente.setTipoDocumento(dto.tipoDoc());
                existente.setNumeroDocumento(dto.documento());

                if (existente instanceof PersonaHumana humana) {
                    String[] partes = dto.nombreORazonSocial().split(" ", 2);
                    humana.setNombre(partes[0]);
                    humana.setApellido(partes.length > 1 ? partes[1] : humana.getApellido());
                } else if (existente instanceof PersonaJuridica juridica) {
                    juridica.setRazonSocial(dto.nombreORazonSocial());
                }

                donanteRepository.guardar(existente);
                return ImportFilaCSV.actualizado(linea, dto.email());
            } else {
                PersonaDonante nuevo = factory.crearPersona(dto);
                donanteRepository.guardar(nuevo);
                return ImportFilaCSV.creado(linea, dto.email());
            }

        } catch (Exception e) {
            return ImportFilaCSV.error(linea, dto.email(), "Error inesperado: " + e.getMessage());
        }
    }
}
