package ar.utn.donatrack.donaciones.importacion.dto;

/**
 * DTO de transferencia para una fila del CSV de importación masiva.
 * Representa los datos mínimos disponibles en el archivo para identificar
 * y contactar a una persona donante (humana o jurídica).
 */
public record DonanteImportDto(
    int linea,                 // línea del archivo donde empieza el registro
    String tipoPersona,        // "HUMANA" | "JURIDICA"
    String tipoDoc,            // "DNI" | "CUIT"
    String documento,
    String nombreORazonSocial,
    String email,
    String telefono            // puede ser null
) {}
