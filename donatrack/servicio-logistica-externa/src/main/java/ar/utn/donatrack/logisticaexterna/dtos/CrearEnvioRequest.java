package ar.utn.donatrack.logisticaexterna.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Un envío por request. El domicilio viaja como texto libre
 * (ej. "Medrano 951, CABA, Buenos Aires, C1179").
 */
public record CrearEnvioRequest(
        @NotBlank String shipmentRef,
        @NotBlank String destinatario,
        @NotBlank String domicilio) {
}
