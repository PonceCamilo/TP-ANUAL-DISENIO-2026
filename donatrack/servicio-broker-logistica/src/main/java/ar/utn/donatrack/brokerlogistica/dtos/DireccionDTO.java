package ar.utn.donatrack.brokerlogistica.dtos;

import jakarta.validation.constraints.NotBlank;

public record DireccionDTO(
        @NotBlank String calle,
        int numero,
        @NotBlank String localidad,
        @NotBlank String provincia,
        String codigoPostal) {

    /** Formato texto libre, para proveedores que reciben el domicilio en una sola línea. */
    public String enUnaLinea() {
        String linea = calle + " " + numero + ", " + localidad + ", " + provincia;
        return codigoPostal == null || codigoPostal.isBlank() ? linea : linea + ", " + codigoPostal;
    }
}
