package ar.utn.donatrack.donaciones.model.donante;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: STATE
// ───────────────────────────────────────────────────────────────────────────────
// cada estado es explícito y nombrarlo en el código comunica la intención del negocio.
// Agregar un nuevo estado = agregar un valor al enum y las transiciones que
// correspondan, sin tocar nada más.
// Beneficio: el estado del donante es visible, extensible y seguro de chequear
// con un switch exhaustivo.
// ═══════════════════════════════════════════════════════════════════════════════

public enum EstadoDonante {

    // LÍNEAS CLAVE: estados posibles del ciclo de vida de la persona donante
    ACTIVO,      // ← estado inicial al registrarse
    INACTIVO;    // ← estado tras darDeBaja()

    // ══════════════════════════════════════════════════
    // PRÓXIMA ENTREGA: agregar estados intermedios como
    // SUSPENDIDO o PENDIENTE_VERIFICACION cuando el flujo
    // de onboarding y moderación esté definido.
    // ══════════════════════════════════════════════════
}
