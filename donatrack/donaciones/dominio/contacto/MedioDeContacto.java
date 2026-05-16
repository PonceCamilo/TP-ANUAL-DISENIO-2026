package com.donatrack.donaciones.dominio.contacto;

/**
 * Representa un medio a través del cual se puede contactar a una persona donante.
 * Cada subclase concreta define el tipo de canal (email, teléfono, WhatsApp).
 *
 * Decisión de diseño: clase abstracta (no interfaz) porque todos los medios
 * de contacto comparten un estado común: el valor del contacto (dirección de
 * email, número de teléfono, etc.).
 */
public abstract class MedioDeContacto {

    private final String valor;

    protected MedioDeContacto(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El valor del medio de contacto no puede estar vacío.");
        }
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public abstract TipoMedioContacto getTipo();

    @Override
    public String toString() {
        return getTipo() + ": " + valor;
    }
}
