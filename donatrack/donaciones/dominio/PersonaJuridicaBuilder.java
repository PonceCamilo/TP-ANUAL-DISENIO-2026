package com.donatrack.donaciones.dominio;

import com.donatrack.donaciones.dominio.contacto.MedioDeContacto;

import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: BUILDER — PersonaJuridicaBuilder
// ───────────────────────────────────────────────────────────────────────────────
// Mismo razonamiento que PersonaHumanaBuilder: PersonaJuridica tiene listas
// (medios y representantes) que son difíciles de armar inline. Con el builder
// se van agregando uno por uno de forma clara.
// ═══════════════════════════════════════════════════════════════════════════════

public class PersonaJuridicaBuilder {

    // LÍNEAS CLAVE: estado acumulativo del builder
    private String razonSocial;                                         // ← campo builder
    private TipoPersonaJuridica tipo;                                   // ← campo builder
    private String rubro;                                               // ← campo builder
    private final List<MedioDeContacto> mediosDeContacto               // ← campo builder
            = new ArrayList<>();
    private final List<Representante> representantes                    // ← campo builder
            = new ArrayList<>();

    public PersonaJuridicaBuilder razonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
        return this;
    }

    public PersonaJuridicaBuilder tipo(TipoPersonaJuridica tipo) {
        this.tipo = tipo;
        return this;
    }

    public PersonaJuridicaBuilder rubro(String rubro) {
        this.rubro = rubro;
        return this;
    }

    public PersonaJuridicaBuilder medios(List<MedioDeContacto> medios) {
        this.mediosDeContacto.addAll(medios);
        return this;
    }

    public PersonaJuridicaBuilder agregarRepresentante(Representante rep) {
        this.representantes.add(rep);
        return this;
    }

    public PersonaJuridicaBuilder representantes(List<Representante> reps) {
        this.representantes.addAll(reps);
        return this;
    }

    // LÍNEA CLAVE: build() ensambla el objeto con todos los datos acumulados
    public PersonaJuridica build() {                                     // ← método de construcción
        return new PersonaJuridica(razonSocial, tipo, rubro,
                mediosDeContacto, representantes);
    }
}
