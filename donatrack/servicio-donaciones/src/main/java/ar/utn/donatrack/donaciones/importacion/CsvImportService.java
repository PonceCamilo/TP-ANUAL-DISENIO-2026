package com.donatrack.donaciones.importacion;

import com.donatrack.donaciones.dominio.PersonaDonante;
import com.donatrack.donaciones.dominio.TipoMedioContacto;
import com.donatrack.donaciones.dominio.Telefono;
import com.donatrack.donaciones.repositorio.DonanteRepository;
import com.donatrack.donaciones.repositorio.NotificacionService;

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
public class CsvImportService {

    private static final int BATCH_SIZE = 500;

    private final DonanteRepository   donanteRepository;
    private final DonanteCsvRowParser parser;
    private final DonanteFactory      factory;
    private final NotificacionService notificacionService;

    public CsvImportService(DonanteRepository donanteRepository,
                            DonanteCsvRowParser parser,
                            DonanteFactory factory,
                            NotificacionService notificacionService) {
        this.donanteRepository   = donanteRepository;
        this.parser              = parser;
        this.factory             = factory;
        this.notificacionService = notificacionService;
    }

    // ─── Punto de entrada ─────────────────────────────────────────────────────

    public ImportReport importar(InputStream csvStream) throws IOException {
        ImportReport report = new ImportReport();

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
                    report.agregar(ImportResult.error(numeroLinea, "", e.getMessage()));
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
                               ImportReport report,
                               int lineaInicio) {
        int lineaActual = lineaInicio;
        for (DonanteImportDto dto : lote) {
            report.agregar(procesarFila(dto, lineaActual));
            lineaActual++;
        }
    }

    private ImportResult procesarFila(DonanteImportDto dto, int linea) {
        try {
            Optional<PersonaDonante> existente = donanteRepository.findByEmail(dto.email());

            if (existente.isPresent()) {
                actualizarContacto(existente.get(), dto);
                donanteRepository.save(existente.get());
                return ImportResult.actualizado(linea, dto.email());
            } else {
                PersonaDonante nuevo = crearDesdeDto(dto);
                donanteRepository.save(nuevo);
                notificacionService.enviarCredencialesNuevoUsuario(nuevo);
                return ImportResult.creado(linea, dto.email());
            }

        } catch (Exception e) {
            return ImportResult.error(linea, dto.email(),
                    "Error inesperado: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Construye la PersonaDonante correcta según el tipo indicado en el CSV.
     * Usa DonanteFactory para respetar los invariantes del dominio.
     */
    private PersonaDonante crearDesdeDto(DonanteImportDto dto) {
        return switch (dto.tipoPersona()) {
            case "HUMANA"   -> factory.crearHumana(dto);
            case "JURIDICA" -> factory.crearJuridica(dto);
            default -> throw new IllegalArgumentException(
                    "Tipo de persona no soportado: " + dto.tipoPersona());
        };
    }

    /**
     * Actualización mínima: agrega el teléfono si el CSV lo trae
     * y la persona todavía no lo tiene registrado.
     * El email (clave de idempotencia) nunca se modifica.
     */
    private void actualizarContacto(PersonaDonante donante, DonanteImportDto dto) {
        if (dto.telefono() == null) return;

        boolean yaTieneTelefono = donante.getMediosDeContacto().stream()
                .anyMatch(m -> m.getTipo() == TipoMedioContacto.TELEFONO);

        if (!yaTieneTelefono) {
            donante.agregarMedioDeContacto(new Telefono(dto.telefono()));
        }
    }
}
