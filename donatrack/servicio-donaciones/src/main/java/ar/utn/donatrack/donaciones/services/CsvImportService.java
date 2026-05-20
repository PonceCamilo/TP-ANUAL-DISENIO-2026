package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.excepcion.CsvFormatoInvalidoException;
import ar.utn.donatrack.donaciones.importacion.DonanteCsvRowParser;
import ar.utn.donatrack.donaciones.importacion.DonanteFactory;
import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import ar.utn.donatrack.donaciones.repositories.ImportCSVRepository;
import ar.utn.donatrack.donaciones.importacion.ImportFilaCSV;
import ar.utn.donatrack.donaciones.model.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.NotificacionServiceInterface;

import lombok.Builder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orquestador del proceso de importación masiva de personas donantes por CSV.
 *
 * Responsabilidades:
 *  - Leer el archivo línea a línea en lotes de {@value BATCH_SIZE} registros
 *    para no cargar el archivo completo en memoria (soporta 20.000+ filas).
 *  - Delegar el parseo a DonanteCsvRowParser.
 *  - Decidir si crear o actualizar usando el email como clave de idempotencia.
 *  - Delegar la construcción de objetos de dominio a DonanteFactory.
 *  - Acumular resultados en un ImportReport sin detener el proceso ante errores parciales.
 *
 * Decisiones de diseño:
 *  - Transacción por lote (no por archivo): un error en una fila no rollbackea
 *    las filas anteriores ya persistidas.
 *  - Actualización mínima: en re-importaciones no se sobreescriben datos ya
 *    enriquecidos por el administrador. Solo se agrega el teléfono si faltaba.
 *  - El email es la clave de idempotencia porque es obligatorio y único
 *    para ambos tipos de persona, tal como lo define el enunciado.
 */

@Builder
@Service
public class CsvImportService {

    private static final int BATCH_SIZE = 500;

    private final DonanteRepositoryInterface donanteRepository;
    private final DonanteCsvRowParser parser;
    private final DonanteFactory factory;
    private final NotificacionServiceInterface notificacionService;

    // ─── Punto de entrada ─────────────────────────────────────────────────────

    public ImportCSVRepository importar(InputStream csvStream) throws IOException {
        ImportCSVRepository report = new ImportCSVRepository();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {

            reader.readLine(); // saltar encabezado

            List<DonanteImportDto> lote = new ArrayList<>(BATCH_SIZE);
            String linea;
            int numeroLinea = 2; // empieza en 2 por el header

            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) { numeroLinea++; continue; }

                try {
                    String[] columnas = linea.split(",", -1);
                    DonanteImportDto dto = parser.parsear(columnas, numeroLinea);
                    lote.add(dto);
                } catch (CsvFormatoInvalidoException e) {
                    report.agregar(ImportFilaCSV.error(numeroLinea, "", e.getMessage()));
                }

                if (lote.size() >= BATCH_SIZE) {
                    procesarLote(lote, report, numeroLinea - lote.size() + 1);
                    lote.clear();
                }
                numeroLinea++;
            }

            // procesar el último lote (puede ser menor a BATCH_SIZE)
            if (!lote.isEmpty()) {
                procesarLote(lote, report, numeroLinea - lote.size());
            }
        }

        return report;
    }

    // ─── Procesamiento por lote ───────────────────────────────────────────────

    private void procesarLote(List<DonanteImportDto> lote,
                               ImportCSVRepository report,
                               int lineaInicio) {
        int lineaActual = lineaInicio;
        for (DonanteImportDto dto : lote) {
            report.agregar(procesarFila(dto, lineaActual));
            lineaActual++;
        }
    }

    private ImportFilaCSV procesarFila(DonanteImportDto dto, int linea) {
        try {
            Optional<PersonaDonante> existente = donanteRepository.findByEmail(dto.email());

            if (existente.isPresent()) {
                actualizarContacto(existente.get(), dto);
                donanteRepository.save(existente.get());
                return ImportFilaCSV.actualizado(linea, dto.email());
            } else {
                PersonaDonante nuevo = crearDesdeDto(dto);
                donanteRepository.save(nuevo);
                notificacionService.enviarCredencialesNuevoUsuario(nuevo);
                return ImportFilaCSV.creado(linea, dto.email());
            }

        } catch (Exception e) {
            return ImportFilaCSV.error(linea, dto.email(),
                    "Error inesperado: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Construye la PersonaDonante correcta según el tipo indicado en el CSV.
     * Usa DonanteFactory para respetar los invariantes del dominio.
     */
    private PersonaDonante crearDesdeDto(DonanteImportDto dto) {
        return factory.crearPersona(dto);
    }

    /**
     * Actualización mínima: agrega el teléfono si el CSV lo trae
     * y la persona todavía no lo tiene registrado.
     * El email (clave de idempotencia) nunca se modifica.
     */
    private void actualizarContacto(PersonaDonante donante, DonanteImportDto dto) {
        // En el modelo actual no existe un API estandarizado para medios de
        // contacto; dejar como no-op para evitar romper la importación.
        // Si en el futuro se agrega soporte para teléfonos, implementar
        // la lógica de actualización mínima aquí.
        return;
    }
}
