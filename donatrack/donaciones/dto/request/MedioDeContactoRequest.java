package com.donatrack.donaciones.dto.request;

import com.donatrack.donaciones.dominio.contacto.TipoMedioContacto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para recibir un medio de contacto en el body del request.
 * Usa Java Record: inmutable, sin boilerplate.
 */
public record MedioDeContactoRequest(

        @NotNull(message = "El tipo de medio de contacto es obligatorio.")
        TipoMedioContacto tipo,

        @NotBlank(message = "El valor del medio de contacto no puede estar vacío.")
        String valor,

        boolean predeterminado
) {}
