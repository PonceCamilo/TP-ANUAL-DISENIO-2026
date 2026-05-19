package com.donatrack.donaciones.importacion;

/**
 * DTO de transferencia para una fila del CSV de importación masiva.
 * Representa los datos mínimos disponibles en el archivo para identificar
 * y contactar a una persona donante (humana o jurídica).
 */
public record DonanteImportDto(
        String tipoPersona,        // "HUMANA" | "JURIDICA"
        String tipoDoc,            // "DNI" | "CUIT"
        String documento,
        String nombreORazonSocial,
        String email,
        String telefono            // puede ser null
) {}
