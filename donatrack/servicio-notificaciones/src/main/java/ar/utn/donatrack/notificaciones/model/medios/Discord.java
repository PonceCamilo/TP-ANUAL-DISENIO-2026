package ar.utn.donatrack.notificaciones.model.medios;

/**
 * Nuevo canal agregado a pedido del profesor: integración con una API
 * real (Discord) además de Email. Al ser un medio más dentro del
 * esquema Strategy, agregarlo no requirió tocar el Service, la Factory
 * ni el DTO de entrada — solo esta clase y su notificador concreto.
 */
public class Discord extends MedioNotificacion {
    @Override
    public String getNombre() { return "DISCORD"; }
}