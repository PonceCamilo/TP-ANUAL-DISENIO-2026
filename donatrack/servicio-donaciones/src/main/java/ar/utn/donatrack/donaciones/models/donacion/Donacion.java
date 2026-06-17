package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Donacion {

    protected List<Bien> bienes = new ArrayList<>();
    protected Subcategoria subcategoria;
    protected EstadoDonacion estado = EstadoDonacion.EN_DEPOSITO;
    protected List<CambioEstado> historialEstados = new ArrayList<>();
    protected UUID idDonante;
    protected UUID id = UUID.randomUUID();

    public boolean esPerecible() {
        return !bienes.isEmpty() && bienes.getFirst() instanceof BienPerecible;
    }

    public boolean requiereEstado() {
        return !bienes.isEmpty() && bienes.getFirst() instanceof BienConEstado;
    }
}