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
 
    private String razonSocial;                                         
    private TipoPersonaJuridica tipo;                                   
    private String rubro;                                               
    private final List<MedioDeContacto> mediosDeContacto               
            = new ArrayList<>();
    private final List<Representante> representantes                    
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
 
    public PersonaJuridica build() {                                     
        return new PersonaJuridica(razonSocial, tipo, rubro,
                mediosDeContacto, representantes);
    }
}
