package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.models.contacto.Canal;
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
import ar.utn.donatrack.donaciones.util.NormalizadorEmail;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvMalformedLineException;
import com.opencsv.exceptions.CsvValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Objects;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final PersonaDonanteRepositoryInterface donanteRepository;
    private final DonanteCsvRowParser parser;
    private final DonanteFactory factory;
    private final DonacionesProperties propiedades;

    /**
     * Cada lote (y, en el camino de respaldo, cada fila) se procesa en una
     * transacción propia y no en una sola para todo el archivo: un archivo de
     * miles de filas no puede quedar sin importar por una fila mala, y lo ya
     * importado queda confirmado aunque la lectura se corte después.
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
        int linea = 1;

        try (CSVReader lector = new CSVReaderBuilder(
            new InputStreamReader(csvStream, StandardCharsets.UTF_8)).build()) {

            lector.readNext(); // encabezado
            linea = Math.toIntExact(lector.getLinesRead()) + 1;
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

        } catch (CsvValidationException | CsvMalformedLineException e) {
            // Los lotes anteriores ya se guardaron: en lugar de responder un error
            // sin decir qué se importó, se informa dónde se cortó la lectura y se
            // devuelve el informe de todo lo procesado hasta ahí.
            log.warn("[IMPORTACION] Lectura interrumpida en la línea {}: {}", linea, e.getMessage());
            report.agregar(ImportFilaCSV.error(linea, "",
                    "Lectura interrumpida en esta línea, el resto del archivo no se procesó: " + e.getMessage()));
        }

        if (!lote.isEmpty()) {
            procesarLote(lote, report);
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
     * demás: el error queda en el reporte solo para esa fila. Lo mismo pasa si
     * una fila falla de forma inesperada en el camino rápido: se aborta la
     * transacción del lote (así ninguna persona queda guardada con cambios a
     * medias) y se reprocesa fila por fila. La causa queda en el log.
     */
    private void procesarLote(List<DonanteImportDto> lote, ImportReport report) {
        List<ImportFilaCSV> resultados;
        try {
            resultados = Objects.requireNonNull(
                    transaccion.execute(estado -> procesarLoteEnUnaTransaccion(lote)),
                    "el lote no devolvió resultados");
        } catch (Exception errorDelLote) {
            log.warn("[IMPORTACION] El lote que empieza en la línea {} no se pudo guardar junto; "
                    + "se reprocesa fila por fila.", lote.getFirst().linea(), errorDelLote);
            resultados = lote.stream()
                    .map(dto -> Objects.requireNonNull(transaccion.execute(estado -> procesarFila(dto))))
                    .toList();
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
            PersonaDonante existente = porEmail.get(clave);
            String conflicto = existente == null ? null : conflictoDeTipo(existente, dto);
            if (conflicto != null) {
                resultados.add(ImportFilaCSV.error(linea, dto.email(), conflicto));
            } else if (existente != null) {
                actualizar(existente, dto);
                aGuardar.put(clave, existente);
                resultados.add(ImportFilaCSV.actualizado(linea, dto.email()));
            } else {
                PersonaDonante nueva = factory.crearPersona(dto);
                porEmail.put(clave, nueva);
                aGuardar.put(clave, nueva);
                resultados.add(ImportFilaCSV.creado(linea, dto.email()));
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
            String conflicto = existente == null ? null : conflictoDeTipo(existente, dto);
            if (conflicto != null) {
                return ImportFilaCSV.error(linea, dto.email(), conflicto);
            }
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
        existente.actualizarNombreImportado(dto.nombreORazonSocial());

        if (dto.telefono() != null && !dto.telefono().isBlank() && !tieneTelefono(existente, dto.telefono())) {
            existente.reemplazarContacto(Telefono.builder().valor(dto.telefono()).build());
        }
    }

    /**
     * Una fila no puede convertir a una persona humana en jurídica ni al revés:
     * si el email ya pertenece a una persona del otro tipo, la fila es un error y
     * no se modifica nada. Antes la fila se aplicaba igual sobre la persona
     * existente (una fila JURIDICA dejaba la razón social como nombre y apellido
     * de una persona humana).
     */
    private static String conflictoDeTipo(PersonaDonante existente, DonanteImportDto dto) {
        if (existente.tipoPersona().name().equals(dto.tipoPersona())) {
            return null;
        }
        return "El email ya pertenece a una persona " + existente.tipoPersona()
                + " y la fila indica " + dto.tipoPersona() + ": no se modificó.";
    }

    /**
     * Si el teléfono no cambió no se toca: reemplazarlo igual borraría el
     * contacto y crearía otro idéntico, tres sentencias por fila al reimportar.
     */
    private static boolean tieneTelefono(PersonaDonante persona, String telefono) {
        return persona.getContactos().stream()
                .anyMatch(contacto -> contacto.canal() == Canal.SMS && telefono.equals(contacto.getValor()));
    }

}
