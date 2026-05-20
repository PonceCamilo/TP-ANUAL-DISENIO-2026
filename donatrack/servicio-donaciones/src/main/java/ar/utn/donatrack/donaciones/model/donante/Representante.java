package ar.utn.donatrack.donaciones.model.donante;

/**
 * Persona física habilitada para operar en nombre de una persona jurídica.
 * Una persona jurídica puede tener múltiples representantes.
 */
public class Representante {

    private final String nombre;
    private final String apellido;
    private final String email;
    private String telefono; // opcional

    public Representante(String nombre, String apellido, String email, String telefono) {
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre del representante no puede estar vacío.");
        if (apellido == null || apellido.isBlank())
            throw new IllegalArgumentException("El apellido del representante no puede estar vacío.");
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("El email del representante no puede estar vacío.");

        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
    }

    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getNombreCompleto() { return nombre + " " + apellido; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
