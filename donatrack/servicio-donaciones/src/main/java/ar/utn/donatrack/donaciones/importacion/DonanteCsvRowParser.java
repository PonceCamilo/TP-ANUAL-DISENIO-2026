package ar.utn.donatrack.donaciones.importacion;

import ar.utn.donatrack.donaciones.exceptions.csvExceptions.CsvFormatoException;
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
            throw CsvFormatoException.columnasInsuficientes(numeroLinea, columnas.length);
        }

        String tipoPersona = columnas[0].trim().toUpperCase();
        String tipoDoc = columnas[1].trim().toUpperCase();
        String doc     = columnas[2].trim();
        String nombre  = columnas[3].trim();
        String email   = columnas[4].trim();
        String tel     = columnas.length > 5 ? columnas[5].trim() : null;

        if (!tipoPersona.equals("HUMANA") && !tipoPersona.equals("JURIDICA")) {
            throw CsvFormatoException.tipoPersonaInvalido(numeroLinea, tipoPersona);
        }
        if (email.isBlank()) {
            throw CsvFormatoException.emailVacio(numeroLinea);
        }
        if (doc.isBlank()) {
            throw CsvFormatoException.documentoVacio(numeroLinea);
        }
        if (nombre.isBlank()) {
            throw CsvFormatoException.nombreVacio(numeroLinea);
        }

        return new DonanteImportDto(
            numeroLinea, tipoPersona, tipoDoc, doc, nombre, email,
                (tel != null && tel.isBlank()) ? null : tel
        );
    }
}
