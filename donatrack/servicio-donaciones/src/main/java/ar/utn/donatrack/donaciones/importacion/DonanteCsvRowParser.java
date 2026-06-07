package ar.utn.donatrack.donaciones.importacion;

import ar.utn.donatrack.donaciones.exceptions.CsvFormatoInvalidoException;
import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import org.springframework.stereotype.Component;

/**
 * Responsable de parsear y validar una fila del CSV de importación masiva.
 *
 * Formato esperado (separado por comas):
 *   TipoPersona, TipoDoc, Documento, Nombre/RazonSocial, Email, Teléfono(opcional)
 *
 * Decisión de diseño: el parser sólo valida lo que el CSV puede proveer.
 * Los invariantes completos del dominio (edad, género, etc.) se resuelven
 * en DonanteFactory con valores neutros.
 */
@Component
public class DonanteCsvRowParser {

    public DonanteImportDto parsear(String[] columnas, int numeroLinea) {
        if (columnas.length < 5) {
            throw new CsvFormatoInvalidoException(
                    "Línea " + numeroLinea + ": se esperaban al menos 5 columnas, "
                            + "se encontraron " + columnas.length);
        }

        String tipoPersona = columnas[0].trim().toUpperCase();
        String tipoDoc = columnas[1].trim().toUpperCase();
        String doc     = columnas[2].trim();
        String nombre  = columnas[3].trim();
        String email   = columnas[4].trim();
        String tel     = columnas.length > 5 ? columnas[5].trim() : null;

        if (!tipoPersona.equals("HUMANA") && !tipoPersona.equals("JURIDICA")) {
            throw new CsvFormatoInvalidoException(
                    "Línea " + numeroLinea + ": tipo de persona inválido '" + tipoPersona
                            + "'. Valores aceptados: HUMANA, JURIDICA");
        }
        if (email.isBlank()) {
            throw new CsvFormatoInvalidoException(
                    "Línea " + numeroLinea + ": el email es obligatorio");
        }
        if (doc.isBlank()) {
            throw new CsvFormatoInvalidoException(
                    "Línea " + numeroLinea + ": el documento no puede estar vacío");
        }
        if (nombre.isBlank()) {
            throw new CsvFormatoInvalidoException(
                    "Línea " + numeroLinea + ": el nombre/razón social no puede estar vacío");
        }

        return new DonanteImportDto(
            tipoPersona, tipoDoc, doc, nombre, email,
                (tel != null && tel.isBlank()) ? null : tel
        );
    }
}
