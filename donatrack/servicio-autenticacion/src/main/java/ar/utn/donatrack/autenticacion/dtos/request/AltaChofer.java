package ar.utn.donatrack.autenticacion.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AltaChofer(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(min = 8, max = 72) String password) {}
