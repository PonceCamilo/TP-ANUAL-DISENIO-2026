package com.donatrack.donaciones.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RepresentanteRequest(

        @NotBlank(message = "El nombre del representante es obligatorio.")
        String nombre,

        @NotBlank(message = "El apellido del representante es obligatorio.")
        String apellido,

        @NotBlank(message = "El email del representante es obligatorio.")
        @Email(message = "El email del representante no tiene formato válido.")
        String email,

        String telefono // opcional
) {}
