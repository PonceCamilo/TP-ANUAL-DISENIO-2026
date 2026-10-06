package ar.utn.donatrack.donaciones.integracion.logistica;

import ar.utn.donatrack.donaciones.models.entidad.Direccion;

import java.util.UUID;

/**
 * Una donación lista para que un proveedor de logística la incluya en una ruta.
 *
 * Está escrita en el lenguaje de donaciones, no en el de ningún proveedor: cada
 * adapter la traduce al contrato que su proveedor espera. Gracias a eso, sumar
 * un proveedor nuevo no obliga a tocar ni el broker ni el servicio que lo dispara.
 */
public record SolicitudEntrega(
        UUID idDonacion,
        UUID idEntidadBeneficiaria,
        Direccion direccionEntrega) {
}
