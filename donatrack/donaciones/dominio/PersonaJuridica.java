package com.donatrack.donaciones.dominio;

import com.donatrack.donaciones.dominio.contacto.MedioDeContacto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persona donante de tipo jurídico (empresa, ONG, institución, etc.).
 * Debe contar con al menos un representante habilitado para operar.
 */
public class PersonaJuridica extends PersonaDonante {

    private String razonSocial;
    private TipoPersonaJuridica tipo;
    private String rubro;
    private final List<Representante> representantes;

    public PersonaJuridica(String razonSocial,
                           TipoPersonaJuridica tipo,
                           String rubro,
                           List<MedioDeContacto> mediosDeContacto,
                           List<Representante> representantes) {
        super(mediosDeContacto);
        validar(razonSocial, tipo, rubro, representantes);
        this.razonSocial = razonSocial;
        this.tipo = tipo;
        this.rubro = rubro;
        this.representantes = new ArrayList<>(representantes);
    }

    // ─── Métodos de negocio ───────────────────────────────────────────────────

    public void agregarRepresentante(Representante representante) {
        if (representante == null)
            throw new IllegalArgumentException("El representante no puede ser nulo.");
        boolean yaExiste = representantes.stream()
                .anyMatch(r -> r.getEmail().equalsIgnoreCase(representante.getEmail()));
        if (yaExiste) {
            throw new IllegalArgumentException(
                    "Ya existe un representante con el email: " + representante.getEmail());
        }
        representantes.add(representante);
    }

    public void removerRepresentante(String emailRepresentante) {
        boolean removed = representantes.removeIf(
                r -> r.getEmail().equalsIgnoreCase(emailRepresentante));
        if (!removed) {
            throw new IllegalArgumentException(
                    "No se encontró un representante con el email: " + emailRepresentante);
        }
        if (representantes.isEmpty()) {
            throw new IllegalStateException(
                    "La persona jurídica debe tener al menos un representante.");
        }
    }

    public void actualizarDatos(String razonSocial, TipoPersonaJuridica tipo, String rubro) {
        validar(razonSocial, tipo, rubro, this.representantes);
        this.razonSocial = razonSocial;
        this.tipo = tipo;
        this.rubro = rubro;
    }

    // ─── Validaciones ─────────────────────────────────────────────────────────

    private void validar(String razonSocial, TipoPersonaJuridica tipo,
                         String rubro, List<Representante> reps) {
        if (razonSocial == null || razonSocial.isBlank())
            throw new IllegalArgumentException("La razón social no puede estar vacía.");
        if (tipo == null)
            throw new IllegalArgumentException("El tipo de persona jurídica no puede ser nulo.");
        if (rubro == null || rubro.isBlank())
            throw new IllegalArgumentException("El rubro no puede estar vacío.");
        if (reps == null || reps.isEmpty())
            throw new IllegalArgumentException(
                    "La persona jurídica debe tener al menos un representante habilitado.");
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getRazonSocial() { return razonSocial; }
    public TipoPersonaJuridica getTipo() { return tipo; }
    public String getRubro() { return rubro; }
    public List<Representante> getRepresentantes() {
        return Collections.unmodifiableList(representantes);
    }
}
