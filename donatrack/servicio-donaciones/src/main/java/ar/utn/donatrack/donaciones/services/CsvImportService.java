package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.exceptions.csvExcepctions.CsvFormatoException;
import ar.utn.donatrack.donaciones.importacion.DonanteCsvRowParser;
import ar.utn.donatrack.donaciones.importacion.DonanteFactory;
import ar.utn.donatrack.donaciones.importacion.ImportFilaCSV;
import ar.utn.donatrack.donaciones.importacion.ImportReport;
import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.util.NormalizadorEmail;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestador del proceso de importación masiva de personas donantes por CSV.
 *
 * Responsabilidades:
 *  - Leer el archivo línea a línea en lotes de BATCH_SIZE registros, y procesar
 *    cada lote en una transacción, con una consulta y una escritura (Entrega 5).
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

    /**
     * Cada lote se procesa en una transacción propia. Es la única excepción a la
     * regla del servicio de dejar las transacciones en los repositorios: el lote
     * necesita leer y escribir en la misma transacción para no releer a cada
     * persona antes de actualizarla.
     */
    private final TransactionOperations transaccion;

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

    /**
     * Procesa un lote dentro de UNA transacción, con una consulta y una
     * escritura (Entrega 5): trae de una vez a las personas ya registradas con
     * los emails del lote, decide en memoria si cada fila crea o actualiza, y
     * guarda todo al confirmar, con INSERT y UPDATE agrupados por lotes. Como las
     * personas leídas siguen administradas por la misma transacción, actualizar
     * no requiere volver a leerlas. Antes cada fila hacía su propia búsqueda por
     * email y su propio guardado.
     *
     * Si un email se repite dentro del lote, la segunda fila actualiza a la
     * persona que creó la primera: es la misma regla que entre archivos.
     *
     * Si el lote no se puede guardar, se procesa otra vez fila por fila, cada una
     * en su propia transacción, para que una fila mala no se lleve puestas a las
     * demás: el error queda en el reporte solo para esa fila.
     */
    private void procesarLote(List<DonanteImportDto> lote, ImportReport report, int lineaInicio) {
        List<ImportFilaCSV> resultados;
        try {
            resultados = transaccion.execute(estado -> procesarLoteEnUnaTransaccion(lote, lineaInicio));
        } catch (Exception errorDelLote) {
            resultados = new ArrayList<>(lote.size());
            int linea = lineaInicio;
            for (DonanteImportDto dto : lote) {
                resultados.add(procesarFila(dto, linea++));
            }
        }
        resultados.forEach(report::agregar);
    }

    private List<ImportFilaCSV> procesarLoteEnUnaTransaccion(List<DonanteImportDto> lote, int lineaInicio) {
        Map<String, PersonaDonante> porEmail = new HashMap<>(donanteRepository.obtenerPorEmails(
                lote.stream().map(DonanteImportDto::email).toList()));
        Map<String, PersonaDonante> aGuardar = new LinkedHashMap<>();
        List<ImportFilaCSV> resultados = new ArrayList<>(lote.size());

        int linea = lineaInicio;
        for (DonanteImportDto dto : lote) {
            String clave = NormalizadorEmail.normalizar(dto.email());
            try {
                PersonaDonante existente = porEmail.get(clave);
                if (existente != null) {
                    actualizar(existente, dto);
                    aGuardar.put(clave, existente);
                    resultados.add(ImportFilaCSV.actualizado(linea, dto.email()));
                } else {
                    PersonaDonante nueva = factory.crearPersona(dto);
                    porEmail.put(clave, nueva);
                    aGuardar.put(clave, nueva);
                    resultados.add(ImportFilaCSV.creado(linea, dto.email()));
                }
            } catch (Exception e) {
                resultados.add(ImportFilaCSV.error(linea, dto.email(), "Error inesperado: " + e.getMessage()));
            }
            linea++;
        }

        if (!aGuardar.isEmpty()) {
            donanteRepository.guardarTodos(aGuardar.values());
        }
        return resultados;
    }

    /** Camino de respaldo: una fila con su propia búsqueda y su propio guardado. */
    private ImportFilaCSV procesarFila(DonanteImportDto dto, int linea) {
        try {
            PersonaDonante existente = donanteRepository.obtenerPorEmail(dto.email());
            if (existente != null) {
                actualizar(existente, dto);
                donanteRepository.guardar(existente);
                return ImportFilaCSV.actualizado(linea, dto.email());
            }
            donanteRepository.guardar(factory.crearPersona(dto));
            return ImportFilaCSV.creado(linea, dto.email());
        } catch (Exception e) {
            return ImportFilaCSV.error(linea, dto.email(), "Error inesperado: " + e.getMessage());
        }
    }

    /**
     * El email es la clave de idempotencia: nunca se modifica. Se actualizan el
     * resto de los datos identificatorios del CSV y, si la fila trae teléfono,
     * reemplaza el que tenía la persona.
     */
    private void actualizar(PersonaDonante existente, DonanteImportDto dto) {
        existente.setTipoDocumento(dto.tipoDoc());
        existente.setNumeroDocumento(dto.documento());

        if (existente instanceof PersonaHumana humana) {
            String[] partes = dto.nombreORazonSocial().split(" ", 2);
            humana.setNombre(partes[0]);
            humana.setApellido(partes.length > 1 ? partes[1] : humana.getApellido());
        } else if (existente instanceof PersonaJuridica juridica) {
            juridica.setRazonSocial(dto.nombreORazonSocial());
        }

        if (dto.telefono() != null && !dto.telefono().isBlank() && !tieneTelefono(existente, dto.telefono())) {
            existente.reemplazarContacto(Telefono.builder().valor(dto.telefono()).build());
        }
    }

    /**
     * Si el teléfono no cambió no se toca: reemplazarlo igual borraría el
     * contacto y crearía otro idéntico, tres sentencias por fila al reimportar.
     */
    private static boolean tieneTelefono(PersonaDonante persona, String telefono) {
        return persona.getContactos().stream()
                .anyMatch(contacto -> contacto instanceof Telefono && telefono.equals(contacto.getValor()));
    }

}
