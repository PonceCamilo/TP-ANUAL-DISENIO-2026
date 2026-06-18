package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@Getter
@Setter
public class Donacion {

    protected List<Bien> bienes = new ArrayList<>();
    protected Subcategoria subcategoria;
    protected EstadoDonacion estado = EstadoDonacion.EN_DEPOSITO;
    protected List<CambioEstado> historialEstados = new ArrayList<>();
    protected UUID idDonante;
    protected UUID idEntidadBeneficiaria;
    protected String descripcion;
    protected LocalDateTime fechaDonacion = LocalDateTime.now();
    protected UUID id = UUID.randomUUID();
    protected UUID idEntidadAsignada; /** para saber que entindad fue asinada */
    protected LocalDate fechaAsignacion; /** para calcular el ultimo trimestre necesito sabe cuando se asigno*/

    public boolean esPerecible() {
        return !bienes.isEmpty() && bienes.getFirst() instanceof BienPerecible;
    }

    public boolean requiereEstado() {
        return !bienes.isEmpty() && bienes.getFirst() instanceof BienConEstado;
    }
}