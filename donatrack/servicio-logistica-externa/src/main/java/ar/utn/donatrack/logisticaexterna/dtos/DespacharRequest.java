package ar.utn.donatrack.logisticaexterna.dtos;

import jakarta.validation.constraints.NotBlank;

/** vehiculo: patente o identificador del vehículo que lleva el envío. */
public record DespacharRequest(@NotBlank String vehiculo) {
}
