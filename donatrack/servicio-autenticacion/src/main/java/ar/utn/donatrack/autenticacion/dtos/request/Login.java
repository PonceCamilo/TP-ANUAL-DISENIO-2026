package ar.utn.donatrack.autenticacion.dtos.request;

import ar.utn.donatrack.autenticacion.models.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record Login(@NotBlank @Email @Size(max = 150) String email, @NotBlank @Size(max = 72) String password, @NotNull Rol rol) {}
