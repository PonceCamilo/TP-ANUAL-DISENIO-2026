package ar.utn.donatrack.donaciones.dtos.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DireccionRequestDTO {

    @NotBlank
    private String calle;

    @NotNull(message = "El número de calle es obligatorio")
    @Positive
    private Integer numero;

    @NotBlank
    private String codigoPostal;

    @Valid
    @NotNull
    private LocalidadRequestDTO localidad;

    /** Opcional (Entrega 6): ubica la dirección en el mapa de entregas. */
    @DecimalMin(value = "-90", message = "La latitud tiene que estar entre -90 y 90")
    @DecimalMax(value = "90", message = "La latitud tiene que estar entre -90 y 90")
    private Double latitud;

    @DecimalMin(value = "-180", message = "La longitud tiene que estar entre -180 y 180")
    @DecimalMax(value = "180", message = "La longitud tiene que estar entre -180 y 180")
    private Double longitud;

    /** Las coordenadas van juntas: un punto con una sola no se puede ubicar en el mapa. */
    @JsonIgnore
    @AssertTrue(message = "La latitud y la longitud van juntas: indicá las dos o ninguna")
    public boolean isCoordenadasCompletas() {
        return (latitud == null) == (longitud == null);
    }
}
