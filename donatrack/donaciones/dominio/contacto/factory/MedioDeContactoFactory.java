package com.donatrack.donaciones.dominio.contacto.factory;

import com.donatrack.donaciones.dominio.contacto.*;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: FACTORY METHOD
// ───────────────────────────────────────────────────────────────────────────────
// Por qué: Antes, el servicio tenía un switch para instanciar Email/Telefono/WhatsApp.
// Eso significa que cada vez que se agregue un nuevo medio de contacto (ej: Telegram),
// hay que tocar el servicio. Con Factory Method, ese switch vive acá y el servicio
// no sabe nada de cómo se construye cada tipo. Solo pide "dame un medio de tipo X".
//
// Beneficio: Agregar un nuevo canal de contacto = agregar un case acá, nada más.
// ═══════════════════════════════════════════════════════════════════════════════

public class MedioDeContactoFactory {

    // LÍNEA CLAVE: método fábrica estático que centraliza la creación
    public static MedioDeContacto crear(TipoMedioContacto tipo, String valor) {
        return switch (tipo) {                        // ← antes este switch estaba en el Servicio
            case EMAIL    -> new Email(valor);
            case TELEFONO -> new Telefono(valor);
            case WHATSAPP -> new WhatsApp(valor);
        };
    }

    private MedioDeContactoFactory() {
        // No instanciable: es una clase utilitaria de creación
    }
}
