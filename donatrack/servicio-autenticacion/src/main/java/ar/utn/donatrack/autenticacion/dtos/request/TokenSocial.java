package ar.utn.donatrack.autenticacion.dtos.request;

import ar.utn.donatrack.autenticacion.models.Rol;
import ar.utn.donatrack.autenticacion.models.TipoDonante;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TokenSocial(@NotBlank String proveedor, @NotBlank @Size(max = 8192) String idToken, @NotNull Rol rol, TipoDonante tipoDonante) {}
