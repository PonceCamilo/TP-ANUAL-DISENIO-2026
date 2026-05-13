package ar.utn.donatrack.donaciones.model.donacion;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

import ar.utn.donatrack.donaciones.model.donacion.bien.Bien;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Donacion {
    protected Subcategoria subcategoria;
    protected EstadoDonacion estado;
    protected int idCargaOrigen;
    protected List<Bien> bienes = new ArrayList<>();
}
