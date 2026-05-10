package com.donatrack.donaciones.dto.request;

import com.donatrack.donaciones.dominio.TipoPersonaJuridica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegistroPersonaJuridicaRequest(

        @NotBlank(message = "La razón social es obligatoria.")
        String razonSocial,

        @NotNull(message = "El tipo de persona jurídica es obligatorio.")
        TipoPersonaJuridica tipo,

        @NotBlank(message = "El rubro es obligatorio.")
        String rubro,

        @NotNull(message = "Debe proveer al menos un medio de contacto.")
        @Size(min = 1, message = "Debe proveer al menos un medio de contacto.")
        @Valid
        List<MedioDeContactoRequest> mediosDeContacto,

        @NotNull(message = "Debe proveer al menos un representante.")
        @Size(min = 1, message = "Debe proveer al menos un representante.")
        @Valid
        List<RepresentanteRequest> representantes
) {}
