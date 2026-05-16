package com.donatrack.donaciones.dominio;

import com.donatrack.donaciones.dominio.contacto.MedioDeContacto;

import java.util.List;

/**
 * Persona donante de tipo humano (persona física).
 * Atributos requeridos: nombre, apellido, edad, número de documento, género y dirección.
 */
public class PersonaHumana extends PersonaDonante {

    private String nombre;
    private String apellido;
    private int edad;
    private String numeroDocumento;
    private Genero genero;
    private Direccion direccion;

    public PersonaHumana(String nombre,
                         String apellido,
                         int edad,
                         String numeroDocumento,
                         Genero genero,
                         Direccion direccion,
                         List<MedioDeContacto> mediosDeContacto) {
        super(mediosDeContacto);
        validar(nombre, apellido, edad, numeroDocumento, genero, direccion);
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.numeroDocumento = numeroDocumento;
        this.genero = genero;
        this.direccion = direccion;
    }

    // ─── Actualización de datos ───────────────────────────────────────────────

    public void actualizarDatos(String nombre, String apellido, int edad,
                                Genero genero, Direccion direccion) {
        validar(nombre, apellido, edad, this.numeroDocumento, genero, direccion);
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.genero = genero;
        this.direccion = direccion;
    }

    // ─── Validaciones ─────────────────────────────────────────────────────────

    private void validar(String nombre, String apellido, int edad,
                         String documento, Genero genero, Direccion direccion) {
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        if (apellido == null || apellido.isBlank())
            throw new IllegalArgumentException("El apellido no puede estar vacío.");
        if (edad < 0 || edad > 120)
            throw new IllegalArgumentException("La edad debe ser un valor válido.");
        if (documento == null || documento.isBlank())
            throw new IllegalArgumentException("El número de documento no puede estar vacío.");
        if (genero == null)
            throw new IllegalArgumentException("El género no puede ser nulo.");
        if (direccion == null)
            throw new IllegalArgumentException("La dirección no puede ser nula.");
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getNombreCompleto() { return nombre + " " + apellido; }
    public int getEdad() { return edad; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public Genero getGenero() { return genero; }
    public Direccion getDireccion() { return direccion; }
}
