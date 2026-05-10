package com.donatrack.donaciones.dto.request;

import com.donatrack.donaciones.dominio.Genero;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record RegistroPersonaHumanaRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio.")
        String apellido,

        @Min(value = 0, message = "La edad no puede ser negativa.")
        @Max(value = 120, message = "La edad no puede superar 120 años.")
        int edad,

        @NotBlank(message = "El número de documento es obligatorio.")
        String numeroDocumento,

        @NotNull(message = "El género es obligatorio.")
        Genero genero,

        @NotNull(message = "La dirección es obligatoria.")
        @Valid
        DireccionRequest direccion,

        @NotNull(message = "Debe proveer al menos un medio de contacto.")
        @Size(min = 1, message = "Debe proveer al menos un medio de contacto.")
        @Valid
        List<MedioDeContactoRequest> mediosDeContacto
) {}
