package ar.utn.donatrack.donaciones.model.donante;

public class PersonaHumana extends PersonaDonante {
    // TODO: nombre, apellido, edad, documento, direccion
  private String nombre;
  private String apellido;
  private int edad;
  private String documento;

  public PersonaHumana(String nombre, String apellido, int edad, String documento) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.documento = documento;
  }
}
