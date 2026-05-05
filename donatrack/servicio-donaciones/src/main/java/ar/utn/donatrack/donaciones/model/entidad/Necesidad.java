package ar.utn.donatrack.donaciones.model.entidad;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

public abstract class Necesidad {
    // TODO: id, subcategoria, descripcion, entidadBeneficiaria
    protected Subcategoria subcategoria;
    protected String descripcion;

    public abstract boolean estaSatisfecha();
}
