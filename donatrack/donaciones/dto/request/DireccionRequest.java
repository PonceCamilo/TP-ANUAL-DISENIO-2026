package com.donatrack.donaciones.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DireccionRequest(

        @NotBlank(message = "La calle es obligatoria.")
        String calle,

        @NotBlank(message = "El número es obligatorio.")
        String numero,

        @NotBlank(message = "La localidad es obligatoria.")
        String localidad,

        @NotBlank(message = "La provincia es obligatoria.")
        String provincia,

        String codigoPostal
) {}
