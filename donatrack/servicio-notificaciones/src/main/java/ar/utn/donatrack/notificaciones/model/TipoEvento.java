package ar.utn.donatrack.notificaciones.model;

/**
 * Tipos de evento que disparan una notificación, según el enunciado:
 * - INACTIVIDAD_DONANTE: más de 20 días sin interacción.
 * - ASIGNACION_DONACION_ENTIDAD: a la entidad beneficiaria, cuando se le asigna una donación.
 * - ASIGNACION_DONACION_DONANTE: al donante, cuando su donación fue asignada.
 * - MISION_CUMPLIDA: al donante, cuando completa una misión.
 * - CAMBIO_CATEGORIA: al donante, cuando sube de categoría.
 * - INICIO_RUTA: a entidades y donantes involucrados cuando el chofer inicia el recorrido.
 * - ENTREGA_EXITOSA: a la entidad y al donante cuando se confirma la recepción.
 * - ENTREGA_FALLIDA: a la entidad, al donante y a administradores cuando la entrega no se concretó.
 */
public enum TipoEvento {
    INACTIVIDAD_DONANTE,
    ASIGNACION_DONACION_ENTIDAD,
    ASIGNACION_DONACION_DONANTE,
    MISION_CUMPLIDA,
    CAMBIO_CATEGORIA,
    INICIO_RUTA,
    ENTREGA_EXITOSA,
    ENTREGA_FALLIDA
}
