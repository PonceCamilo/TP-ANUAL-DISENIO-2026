package com.donatrack.donaciones.dominio;

import com.donatrack.donaciones.dominio.contacto.MedioDeContacto;
import com.donatrack.donaciones.dominio.contacto.TipoMedioContacto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Raíz de la jerarquía de personas donantes.
 * Una persona donante puede ser humana o jurídica.
 *
 * Invariantes del dominio:
 *  - Debe tener al menos un medio de contacto de tipo EMAIL.
 *  - El medio de contacto predeterminado debe pertenecer a la lista de medios registrados.
 *  - Toda persona donante se crea activa (activo = true).
 */
public abstract class PersonaDonante {

    private final UUID id;
    private final List<MedioDeContacto> mediosDeContacto;
    private MedioDeContacto medioContactoPredeterminado;
    private boolean activo;

    protected PersonaDonante(List<MedioDeContacto> mediosDeContacto) {
        validarMediosDeContacto(mediosDeContacto);
        this.id = UUID.randomUUID();
        this.mediosDeContacto = new ArrayList<>(mediosDeContacto);
        // Por defecto, el email es el medio predeterminado
        this.medioContactoPredeterminado = obtenerEmail();
        this.activo = true;
    }

    // ─── Métodos de negocio ──────────────────────────────────────────────────

    /**
     * Agrega un nuevo medio de contacto a la persona donante.
     * No permite duplicados del mismo tipo y valor.
     */
    public void agregarMedioDeContacto(MedioDeContacto medio) {
        boolean yaExiste = mediosDeContacto.stream()
                .anyMatch(m -> m.getTipo() == medio.getTipo()
                        && m.getValor().equalsIgnoreCase(medio.getValor()));
        if (yaExiste) {
            throw new IllegalArgumentException(
                    "Ya existe un medio de contacto de tipo " + medio.getTipo()
                            + " con el valor '" + medio.getValor() + "'.");
        }
        mediosDeContacto.add(medio);
    }

    /**
     * Establece el medio de contacto predeterminado para notificaciones.
     * El medio debe estar previamente registrado en la persona donante.
     */
    public void establecerMedioContactoPredeterminado(MedioDeContacto medio) {
        boolean estaRegistrado = mediosDeContacto.stream()
                .anyMatch(m -> m.getTipo() == medio.getTipo()
                        && m.getValor().equalsIgnoreCase(medio.getValor()));
        if (!estaRegistrado) {
            throw new IllegalArgumentException(
                    "El medio de contacto no está registrado para esta persona donante.");
        }
        this.medioContactoPredeterminado = medio;
    }

    /** Realiza la baja lógica de la persona donante. */
    public void darDeBaja() {
        if (!this.activo) {
            throw new IllegalStateException("La persona donante ya se encuentra dada de baja.");
        }
        this.activo = false;
    }

    /** Reactiva una persona donante previamente dada de baja. */
    public void reactivar() {
        this.activo = true;
    }

    // ─── Validaciones privadas ───────────────────────────────────────────────

    private void validarMediosDeContacto(List<MedioDeContacto> medios) {
        if (medios == null || medios.isEmpty()) {
            throw new IllegalArgumentException(
                    "La persona donante debe tener al menos un medio de contacto.");
        }
        boolean tieneEmail = medios.stream()
                .anyMatch(m -> m.getTipo() == TipoMedioContacto.EMAIL);
        if (!tieneEmail) {
            throw new IllegalArgumentException(
                    "La persona donante debe tener al menos un correo electrónico.");
        }
    }

    private MedioDeContacto obtenerEmail() {
        return mediosDeContacto.stream()
                .filter(m -> m.getTipo() == TipoMedioContacto.EMAIL)
                .findFirst()
                .orElseThrow(); // nunca llega aquí si pasó la validación
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public UUID getId() { return id; }

    public List<MedioDeContacto> getMediosDeContacto() {
        return Collections.unmodifiableList(mediosDeContacto);
    }

    public MedioDeContacto getMedioContactoPredeterminado() {
        return medioContactoPredeterminado;
    }

    public boolean isActivo() { return activo; }
}
