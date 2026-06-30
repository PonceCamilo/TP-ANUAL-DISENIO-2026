package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CargaDonacion {
    private final UUID idDonante;
    private final String descripcion;
    private final List<Bien> bienes;
}
