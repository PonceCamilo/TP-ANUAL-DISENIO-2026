package ar.utn.donatrack.autenticacion.dtos.request;

import ar.utn.donatrack.autenticacion.models.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pedido de un enlace de activación. Sin rol, se busca la cuenta de donante. */
public record SolicitudActivacion(@NotBlank @Email @Size(max = 150) String email, Rol rol) {
    public Rol rolOPorDefecto() {
        return rol == null ? Rol.DONANTE : rol;
    }
}
