package ar.utn.donatrack.donaciones.model.donante;

public class PersonaHumana extends PersonaDonante {
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
