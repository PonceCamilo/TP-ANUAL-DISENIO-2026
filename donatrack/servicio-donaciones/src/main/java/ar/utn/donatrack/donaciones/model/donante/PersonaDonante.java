package ar.utn.donatrack.donaciones.model.donante;

import ar.utn.donatrack.donaciones.model.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.model.contacto.TipoMedioContacto;
import ar.utn.donatrack.donaciones.model.entidad.Direccion;
import ar.utn.donatrack.donaciones.model.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;
import ar.utn.donatrack.donaciones.model.donante.EstadoDonante;


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
 *  - Toda persona donante se crea en estado ACTIVO.
 *
 * PATRÓN STATE aplicado: el ciclo de vida se modela con EstadoDonante en lugar
 * de un boolean activo. Ver EstadoDonante.java para la justificación completa.
 */
public abstract class PersonaDonante {

    protected UUID id;
    protected List<MedioDeContacto> mediosDeContacto;
    protected MedioDeContacto medioContactoPredeterminado;
    protected EstadoDonante estado;
    protected List<Donacion> donaciones;
    protected CargaDonacion cargaDonacion;
    protected Direccion direccion;

 public PersonaDonante(List<MedioDeContacto> mediosDeContacto) {
        validarMediosDeContacto(mediosDeContacto);
        this.id = UUID.randomUUID();
        this.mediosDeContacto = new ArrayList<>(mediosDeContacto);
        this.medioContactoPredeterminado = obtenerEmail();
        //  el estado inicial es ACTIVO, no "activo = true"
        this.estado = EstadoDonante.ACTIVO;                          
    }

       // ─── Métodos de negocio ──────────────────────────────────────────────────

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

    /**
     * Realiza la baja lógica de la persona donante.
     * PATRÓN STATE: valida la transición ACTIVO → INACTIVO.
     */
    public void darDeBaja() {
        // (STATE): la transición se valida comparando estados
        if (this.estado == EstadoDonante.INACTIVO) {                // ← transición de estado
            throw new IllegalStateException("La persona donante ya se encuentra dada de baja.");
        }
        this.estado = EstadoDonante.INACTIVO;                       // ← cambio de estado
    }

    /**
     * Reactiva una persona donante previamente dada de baja.
     * PATRÓN STATE: transición INACTIVO → ACTIVO.
     */
    public void reactivar() {
        // STATE: transición inversa explícita
        this.estado = EstadoDonante.ACTIVO;                         // ← cambio de estado
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
                .orElseThrow();
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public UUID getId() { return id; }

    public List<MedioDeContacto> getMediosDeContacto() {
        return Collections.unmodifiableList(mediosDeContacto);
    }

    public MedioDeContacto getMedioContactoPredeterminado() {
        return medioContactoPredeterminado;
    }

    // patron STATE: getter del estado tipado — reemplaza isActivo()
    public EstadoDonante getEstado() { return estado; }             // ← getter del estado

    /** Compatibilidad semántica: sigue siendo útil para filtrar activos. */
    public boolean isActivo() { return estado == EstadoDonante.ACTIVO; }
}
