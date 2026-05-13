package ar.utn.donatrack.donaciones.model.entidad;

import ar.utn.donatrack.donaciones.model.categoria.Subcategoria;

public abstract class Necesidad {
    private int id;
    protected Subcategoria subcategoria;
    protected String descripcion;
    private EntidadBeneficiaria entidadBeneficiaria;

    public abstract boolean estaSatisfecha();
}