package ar.utn.donatrack.autenticacion.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Aviso de Donaciones con las personas que cargó la administración. Mismo contrato
 * que AutenticacionClient.CuentasImportadas de servicio-donaciones.
 *
 * Cada persona se valida por separado (CuentasImportadasService): un email mal
 * escrito no rechaza la tanda entera.
 */
public record CuentasImportadas(@NotNull @Size(max = 200) List<Donante> donantes) {
    public record Donante(UUID donanteId, String email, String nombre, String tipoDonante) {}
}
