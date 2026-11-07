package ar.utn.donatrack.logisticaexterna.dtos;

import jakarta.validation.constraints.NotBlank;

public record FallarRequest(@NotBlank String motivo) {
}
