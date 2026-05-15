package com.donatrack.donaciones.dominio;

import com.donatrack.donaciones.dominio.contacto.MedioDeContacto;

import java.util.List;
import java.util.ArrayList;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: BUILDER — PersonaHumanaBuilder => para la justificacion de diseño
// ───────────────────────────────────────────────────────────────────────────────
// Por qué: PersonaHumana tiene 7 parámetros en el constructor. Eso hace que quien
// llama tenga que recordar el orden exacto y es propenso a errores (ej: confundir
// nombre con apellido). Con Builder, cada campo se setea con nombre explícito.
//
// Beneficio: Código más legible, menos errores, y si en el futuro se agregan campos
// opcionales (ej: fechaNacimiento), no rompemos constructores existentes.
// ═══════════════════════════════════════════════════════════════════════════════
public class PersonaHumanaBuilder {
 
    // LÍNEAS CLAVE: campos del builder (algunos con valores por defecto opcionales)
    private String nombre;                                          // ← campo builder
    private String apellido;                                        // ← campo builder
    private int edad;                                               // ← campo builder
    private String numeroDocumento;                                 // ← campo builder
    private Genero genero;                                          // ← campo builder
    private Direccion direccion;                                     // ← campo builder
    private final List<MedioDeContacto> mediosDeContacto           // ← campo builder
            = new ArrayList<>();
 
    public PersonaHumanaBuilder nombre(String nombre) {
        this.nombre = nombre;
        return this;
    }
 
    public PersonaHumanaBuilder apellido(String apellido) {
        this.apellido = apellido;
        return this;
    }
 
    public PersonaHumanaBuilder edad(int edad) {
        this.edad = edad;
        return this;
    }
 
    public PersonaHumanaBuilder numeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
        return this;
    }
 
    public PersonaHumanaBuilder genero(Genero genero) {
        this.genero = genero;
        return this;
    }
 
    public PersonaHumanaBuilder direccion(Direccion direccion) {
        this.direccion = direccion;
        return this;
    }
 
    public PersonaHumanaBuilder agregarMedio(MedioDeContacto medio) {
        this.mediosDeContacto.add(medio);
        return this;
    }
 
    public PersonaHumanaBuilder medios(List<MedioDeContacto> medios) {
        this.mediosDeContacto.addAll(medios);
        return this;
    }
 
    // LÍNEA CLAVE: build() es el único punto donde se construye el objeto real
    public PersonaHumana build() {                                  // ← método de construcción
        return new PersonaHumana(nombre, apellido, edad,
                numeroDocumento, genero, direccion, mediosDeContacto);
    }
}
