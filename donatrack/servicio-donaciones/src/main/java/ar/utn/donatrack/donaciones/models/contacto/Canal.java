package ar.utn.donatrack.donaciones.models.contacto;

/**
 * Canal por el que el Servicio de Notificaciones entrega un mensaje. Los nombres
 * son los que espera ese servicio en el campo "medio".
 */
public enum Canal {
    EMAIL,
    SMS,
    WHATSAPP
}
