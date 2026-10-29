package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donacion.bien.TipoBien;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class BienResponseDTO {
    private final UUID id;
    private final TipoBien tipo;
    private final String subcategoria;
    private final String descripcion;
    private final String foto;
    private final int cantidad;
    private final String unidad;

    // Solo presente si el bien es BienConEstado
    private final Boolean esNuevo;

    // Solo presente si el bien es BienPerecible
    private final LocalDate fechaVencimiento;
}
