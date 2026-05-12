package ar.utn.donatrack.donaciones.model.entidad;

import java.util.List;

public class EntidadBeneficiaria {
    // TODO: id, razonSocial, direccion, telefono, correosRepresentantes
    private int id;
    private String razonSocial;
    private String direccion; // ver si es string o si hacemos una clase direccion
    private String telefono;
    private List<String> correosRepresentantes;
    private List<Necesidad> necesidades;
    

}
