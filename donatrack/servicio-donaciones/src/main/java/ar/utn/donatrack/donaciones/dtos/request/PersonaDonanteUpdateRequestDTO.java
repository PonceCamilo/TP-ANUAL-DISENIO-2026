package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.donante.Genero;
import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para la actualización general (PUT) de una persona donante.
 * Los campos son opcionales según el subtipo (humana o jurídica);
 * el service aplica solo los que correspondan al tipo real del donante.
 * Se pueden corregir los mismos datos que se cargan en el alta, género y tipo
 * de organización incluidos.
 */
@Getter
@Setter
@NoArgsConstructor
public class PersonaDonanteUpdateRequestDTO {

  // Comunes
  @Valid
  private DireccionRequestDTO direccion;

  @Valid
  private List<MedioDeContactoRequestDTO> contactos;

  // Persona humana
  private String nombre;
  private String apellido;
  private LocalDate fechaNacimiento;
  private Genero genero;

  // Persona jurídica
  private String razonSocial;
  private String rubro;
  private TipoPersonaJuridica tipoOrganizacion;
}
