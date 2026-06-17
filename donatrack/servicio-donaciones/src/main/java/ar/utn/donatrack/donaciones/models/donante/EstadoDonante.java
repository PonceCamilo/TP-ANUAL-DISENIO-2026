package ar.utn.donatrack.donaciones.models.donante;

public enum EstadoDonante {

    // LÍNEAS CLAVE: estados posibles del ciclo de vida de la persona donante
    ACTIVO,      // ← estado inicial al registrarse
    INACTIVO,    // ← estado tras darDeBaja()
    BLOQUEADO;

    // ══════════════════════════════════════════════════
    // PRÓXIMA ENTREGA: agregar estados intermedios como
    // SUSPENDIDO o PENDIENTE_VERIFICACION cuando el flujo
    // de onboarding y moderación esté definido.
    // ══════════════════════════════════════════════════
}
