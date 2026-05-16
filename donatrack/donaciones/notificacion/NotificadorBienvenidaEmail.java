package com.donatrack.donaciones.notificacion;

import com.donatrack.donaciones.dominio.PersonaDonante;
import com.donatrack.donaciones.dominio.contacto.TipoMedioContacto;
import org.springframework.stereotype.Component;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: OBSERVER — implementación concreta del suscriptor
// ───────────────────────────────────────────────────────────────────────────────
// Esta clase es uno de los "observadores" que reacciona al evento de registro.
// Su única responsabilidad: enviar el email de bienvenida.
//
// En Entrega 1 solo loguea (simula el envío). En entregas futuras se conectará
// al Servicio de Notificaciones real mediante HTTP o mensajería asíncrona.
// ═══════════════════════════════════════════════════════════════════════════════

// ══════════════════════════════════════════════════════
// PRÓXIMA ENTREGA: conectar este observer al Servicio de
// Notificaciones real (HTTP REST o cola de mensajes).
// Por ahora solo simula el envío con un log.
// ══════════════════════════════════════════════════════

@Component
public class NotificadorBienvenidaEmail implements ObservadorDeRegistro { // ← observer concreto

    // LÍNEA CLAVE: implementa el callback del observer
    @Override
    public void alRegistrarDonante(PersonaDonante donante) {          // ← reacción al evento
        String email = donante.getMediosDeContacto().stream()
                .filter(m -> m.getTipo() == TipoMedioContacto.EMAIL)
                .findFirst()
                .map(m -> m.getValor())
                .orElse("sin-email");

        // Simulación del envío — reemplazar por llamada real en Entrega 4/5
        System.out.println("[NOTIFICACION] Email de bienvenida enviado a: " + email);
    }
}
