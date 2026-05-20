package ar.utn.donatrack.donaciones.model.donante;

import ar.utn.donatrack.donaciones.model.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.model.entidad.Direccion;
import ar.utn.donatrack.donaciones.model.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;
import ar.utn.donatrack.donaciones.model.donante.EstadoDonante;
import ar.utn.donatrack.donaciones.model.contacto.Email;
import ar.utn.donatrack.donaciones.model.contacto.Telefono;
import ar.utn.donatrack.donaciones.model.contacto.WhatsApp;
import ar.utn.donatrack.donaciones.model.contacto.TipoMedioContacto;
import ar.utn.donatrack.donaciones.model.entidad.Necesidad;
import ar.utn.donatrack.donaciones.model.entidad.NecesidadRecurrente;  


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

    // estado acumulativo del builder
    private String razonSocial;                                         
    private TipoPersonaJuridica tipo;                                   
    private String rubro; 
    private Direccion direccion;                                              
    private final List<MedioDeContacto> mediosDeContacto               
            = new ArrayList<>();
    private final List<Representante> representantes                    
            = new ArrayList<>();

    public PersonaJuridicaBuilder razonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
        return this;
    }

    public PersonaJuridicaBuilder direccion(Direccion direccion) {
        this.direccion = direccion;
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

    // build() ensambla el objeto con todos los datos acumulados
    public PersonaJuridica build() {                                   
        return new PersonaJuridica(razonSocial, tipo, rubro,
                mediosDeContacto, direccion, representantes);
    }
}
