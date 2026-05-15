package com.donatrack.donaciones.dominio;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: STATE
// ───────────────────────────────────────────────────────────────────────────────
// Por qué: antes PersonaDonante tenía un "boolean activo". Eso funciona si solo
// hay dos estados, pero el dominio ya tiene darDeBaja() y reactivar(), y en el
// futuro puede necesitar estados como SUSPENDIDO, PENDIENTE_VERIFICACION, etc.
// Con un boolean habría que agregar más flags y la lógica se vuelve confusa.
//
// Con STATE (representado como enum), cada estado es explícito y nombrarlo en
// el código comunica la intención del negocio. Agregar un nuevo estado = agregar
// un valor al enum y las transiciones que correspondan, sin tocar nada más.
//
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
