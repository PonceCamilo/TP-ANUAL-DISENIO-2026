package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.response.ImportacionResumenResponseDTO;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.exceptions.csvExceptions.CsvFormatoException;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestador del proceso de importación masiva de personas donantes por CSV.
 *
 * Responsabilidades:
 *  - Leer el archivo línea a línea en lotes (donaciones.importacion.tamanio-lote, 500 por defecto), y procesar
 *    cada lote en una transacción, con una consulta y una escritura (Entrega 5).
 *  - Delegar el parseo a DonanteCsvRowParser.
 *  - Decidir si crear o actualizar usando el email como clave de idempotencia.
 *  - Delegar la construcción de objetos de dominio a DonanteFactory.
 *  - Acumular resultados en un ImportReport sin detener el proceso ante errores parciales.
 */

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final PersonaDonanteRepositoryInterface donanteRepository;
    private final DonanteCsvRowParser parser;
    private final DonanteFactory factory;
    private final DonacionesProperties propiedades;

    /**
     * Cada lote se procesa en una transacción propia. Es la única excepción a la
     * regla del servicio de dejar las transacciones en los repositorios: el lote
     * necesita leer y escribir en la misma transacción para no releer a cada
     * persona antes de actualizarla.
     */
    private final TransactionOperations transaccion;

    /**
     * Importa y devuelve el resumen que expone la API: las filas correctas se
     * cuentan y de los errores se detallan los primeros
     * (donaciones.importacion.max-errores-en-respuesta).
     */
    public ImportacionResumenResponseDTO importarYResumir(InputStream csvStream) throws IOException {
        return ImportacionResumenResponseDTO.desde(importar(csvStream),
                propiedades.importacion().maxErroresEnRespuesta());
    }

    /**
     * Lee el archivo y lo procesa por lotes.
     *
     * Cada fila guarda su propio número de línea al parsearse: antes la línea se
     * reconstruía con la posición dentro del lote, y una fila con error de formato
     * en el medio corría la numeración de las siguientes. La línea es la física
     * del archivo donde empieza el registro (un campo entre comillas puede ocupar
     * varias), así que coincide con la que se ve al abrir el CSV.
     */
    public ImportReport importar(InputStream csvStream) throws IOException {
        ImportReport report = new ImportReport();
        int tamanioLote = propiedades.importacion().tamanioLote();
        List<DonanteImportDto> lote = new ArrayList<>(tamanioLote);

        try (CSVReader lector = new CSVReaderBuilder(
            new InputStreamReader(csvStream, StandardCharsets.UTF_8)).build()) {

            lector.readNext(); // encabezado
            int linea = Math.toIntExact(lector.getLinesRead()) + 1;
            String[] columnas;

            while ((columnas = lector.readNext()) != null) {
                if (!esLineaEnBlanco(columnas)) {
                    agregarSiEsValida(columnas, linea, lote, report);
                }
                if (lote.size() >= tamanioLote) {
                    procesarLote(lote, report);
                    lote.clear();
                }
                linea = Math.toIntExact(lector.getLinesRead()) + 1;
            }

            if (!lote.isEmpty()) {
                procesarLote(lote, report);
            }

        } catch (CsvValidationException e) {
            throw new IOException("Error al leer el archivo CSV: " + e.getMessage(), e);
        }

        return report;
    }

    /**
     * Una línea vacía (o que solo tiene separadores) no es un registro: se saltea
     * sin informarla. OpenCSV la devuelve como una columna vacía y no como cero
     * columnas, por eso la comparación con length == 0 que había antes nunca se
     * cumplía y cada línea en blanco terminaba informada como error.
     */
    private static boolean esLineaEnBlanco(String[] columnas) {
        return Arrays.stream(columnas).allMatch(columna -> columna == null || columna.isBlank());
    }

    /** Agrega la fila al lote, o la informa como error si no tiene el formato esperado. */
    private void agregarSiEsValida(String[] columnas, int linea, List<DonanteImportDto> lote, ImportReport report) {
        try {
            lote.add(parser.parsear(columnas, linea));
        } catch (CsvFormatoException e) {
            report.agregar(ImportFilaCSV.error(linea, "", e.getMessage()));
        }
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
    private void procesarLote(List<DonanteImportDto> lote, ImportReport report) {
        List<ImportFilaCSV> resultados;
        try {
            resultados = transaccion.execute(estado -> procesarLoteEnUnaTransaccion(lote));
        } catch (Exception errorDelLote) {
            resultados = lote.stream().map(this::procesarFila).toList();
        }
        resultados.forEach(report::agregar);
    }

    private List<ImportFilaCSV> procesarLoteEnUnaTransaccion(List<DonanteImportDto> lote) {
        Map<String, PersonaDonante> porEmail = new HashMap<>(donanteRepository.obtenerPorEmails(
                lote.stream().map(DonanteImportDto::email).toList()));
        Map<String, PersonaDonante> aGuardar = new LinkedHashMap<>();
        List<ImportFilaCSV> resultados = new ArrayList<>(lote.size());

        for (DonanteImportDto dto : lote) {
            int linea = dto.linea();
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
        }

        if (!aGuardar.isEmpty()) {
            donanteRepository.guardarTodos(aGuardar.values());
        }
        return resultados;
    }

    /** Camino de respaldo: una fila con su propia búsqueda y su propio guardado. */
    private ImportFilaCSV procesarFila(DonanteImportDto dto) {
        int linea = dto.linea();
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
