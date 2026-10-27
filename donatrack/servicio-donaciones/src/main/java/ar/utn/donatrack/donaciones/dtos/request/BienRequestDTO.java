package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.donacion.bien.TipoBien;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Un bien de una carga de donación.
 *
 * El tipo se puede indicar explicitamente en "tipo" (PERECIBLE, CON_ESTADO o
 * GENERICO). Si no se indica, se deduce como siempre: con fechaVencimiento es
 * perecedero, con esNuevo es un bien con estado y sin ninguno de los dos es
 * genérico. Lo que ya no se acepta es la combinación ambigua: antes, un bien con
 * fechaVencimiento y esNuevo a la vez se tomaba como perecedero y esNuevo se
 * descartaba sin avisar.
 */
@Getter
@Setter
@NoArgsConstructor
public class BienRequestDTO {
  private TipoBien tipo;
  @NotBlank
  private String subcategoria;
  private String descripcion;
  private String foto;
  @Positive
  private int cantidad;
  @NotBlank
  private String unidad;
  private Boolean esNuevo;
  private LocalDate fechaVencimiento;

  /** El tipo indicado o, si no se indicó, el que se deduce de los campos presentes. */
  @JsonIgnore
  public TipoBien tipoResuelto() {
    if (tipo != null) {
      return tipo;
    }
    if (fechaVencimiento != null) {
      return TipoBien.PERECIBLE;
    }
    return esNuevo != null ? TipoBien.CON_ESTADO : TipoBien.GENERICO;
  }

  /**
   * Cada tipo trae exactamente su dato propio: el perecedero la fecha de
   * vencimiento, el bien con estado si es nuevo, y el genérico ninguno.
   */
  @JsonIgnore
  @AssertTrue(message = "la combinación de datos no corresponde al tipo de bien: un perecedero lleva "
      + "fechaVencimiento, un bien con estado lleva esNuevo y un genérico ninguno de los dos "
      + "(si se mandan los dos, hay que indicar el tipo y solo el dato que le corresponde)")
  public boolean isDatosCoherentesConElTipo() {
    return switch (tipoResuelto()) {
      case PERECIBLE -> fechaVencimiento != null && esNuevo == null;
      case CON_ESTADO -> esNuevo != null && fechaVencimiento == null;
      case GENERICO -> esNuevo == null && fechaVencimiento == null;
    };
  }
}
