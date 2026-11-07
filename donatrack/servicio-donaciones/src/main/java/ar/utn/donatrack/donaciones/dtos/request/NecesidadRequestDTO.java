package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CambioTipoNecesidadException;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.TipoNecesidad;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Alta o modificación de una necesidad. Cada subtipo sabe crear su necesidad y
 * aplicarse sobre una existente: sumar un tipo de necesidad no obliga a tocar el
 * service ni el mapper (antes había parejas de instanceof DTO/modelo).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NecesidadExtraordinariaRequestDTO.class, name = "EXTRAORDINARIA"),
        @JsonSubTypes.Type(value = NecesidadRecurrenteRequestDTO.class, name = "RECURRENTE")
})
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class NecesidadRequestDTO {
    @NotBlank(message = "El nombre de la necesidad es obligatorio")
    private String nombre;

    private String descripcion;

    /** Lo que se necesita (sillas, ropa de abrigo, arroz…). Opcional. */
    private String subcategoria;

    @NotNull(message = "La cantidad objetivo es obligatoria")
    @Min(value = 1, message = "La cantidad objetivo debe ser mayor a 0")
    private Integer cantidadObjetivo;

    public abstract TipoNecesidad tipo();

    /** La necesidad nueva, registrada en esa fecha y con lo recibido en cero. */
    public abstract Necesidad crear(LocalDate hoy);

    /**
     * Aplica los datos sobre una necesidad existente (PUT). No permite cambiar el
     * tipo ni toca lo recibido, que lo registran las entregas.
     */
    public void aplicarEn(Necesidad necesidad) {
        if (necesidad.tipo() != tipo()) {
            throw new CambioTipoNecesidadException(necesidad.tipo().name(), tipo().name());
        }
        necesidad.actualizarDatos(nombre, descripcion, cantidadObjetivo, subcategoriaDelModelo());
    }

    protected <N extends Necesidad> N completar(N necesidad, LocalDate hoy) {
        necesidad.actualizarDatos(nombre, descripcion, cantidadObjetivo, subcategoriaDelModelo());
        necesidad.setCantidadRecibida(0);
        necesidad.setFechaRegistro(hoy);
        return necesidad;
    }

    private Subcategoria subcategoriaDelModelo() {
        return subcategoria == null || subcategoria.isBlank() ? null : new Subcategoria(subcategoria);
    }
}
