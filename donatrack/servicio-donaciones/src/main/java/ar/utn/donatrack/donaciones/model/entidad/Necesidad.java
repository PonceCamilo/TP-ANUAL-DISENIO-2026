package ar.utn.donatrack.donaciones.model.entidad;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

public abstract class Necesidad {
    protected int id;
    protected Subcategoria subcategoria;
    protected String descripcion;
    protected EntidadBeneficiaria entidadBeneficiaria;

    public abstract boolean estaSatisfecha();
}