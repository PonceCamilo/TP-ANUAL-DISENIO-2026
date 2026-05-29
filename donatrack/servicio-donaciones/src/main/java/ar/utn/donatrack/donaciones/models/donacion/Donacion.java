package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;

import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Donacion {
    protected Bien bien;
    protected EstadoDonacion estado;
    protected int idCargaOrigen;
    protected int cantidad;
}
