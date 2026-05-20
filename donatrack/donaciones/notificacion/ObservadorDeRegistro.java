package com.donatrack.donaciones.notificacion;

import com.donatrack.donaciones.dominio.PersonaDonante;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: OBSERVER — interfaz del suscriptor
// ───────────────────────────────────────────────────────────────────────────────
// Por qué: cuando se registra un donante, hay que enviar un email de bienvenida.
// Pero en el futuro puede necesitarse también registrar en un log, enviar SMS,
// notificar al servicio de incentivos, etc. Si ponemos esas llamadas hardcodeadas
// en el servicio, cada nueva acción lo rompe. Con Observer, el servicio solo avisa
// "se registró alguien" y quien quiera reaccionar se suscribe.
//
// Beneficio: agregar nuevas reacciones al registro no toca el servicio principal.
// ═══════════════════════════════════════════════════════════════════════════════

public interface ObservadorDeRegistro {                               

    // LÍNEA CLAVE: método que el sujeto llama para notificar a los suscriptores
    void alRegistrarDonante(PersonaDonante donante);                 
}
