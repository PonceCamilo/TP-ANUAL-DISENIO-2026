package ar.utn.donatrack.donaciones.model.donante;

public class PersonaJuridica extends PersonaDonante {
  private String razonSocial;
  private String rubro;

  public PersonaJuridica(String razonSocial, String rubro) {
    this.razonSocial = razonSocial;
    this.rubro = rubro;
  }
}
