package ar.utn.donatrack.autenticacion.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Código del enlace y la contraseña que eligió la persona. */
public record ConfirmacionActivacion(@NotBlank @Size(max = 100) String codigo,
                                     @NotBlank @Size(min = 8, max = 72) String password) {}
