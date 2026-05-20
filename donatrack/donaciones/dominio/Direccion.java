package com.donatrack.donaciones.dominio;

/**
 * Objeto de valor que representa una dirección postal.
 * Inmutable: no tiene setters. Si la dirección cambia, se crea una nueva instancia.
 */
public class Direccion {

    private final String calle;
    private final String numero;
    private final String localidad;
    private final String provincia;
    private final String codigoPostal;

    public Direccion(String calle, String numero, String localidad,
                     String provincia, String codigoPostal) {
        this.calle = calle;
        this.numero = numero;
        this.localidad = localidad;
        this.provincia = provincia;
        this.codigoPostal = codigoPostal;
    }

    public String getCalle() { return calle; }
    public String getNumero() { return numero; }
    public String getLocalidad() { return localidad; }
    public String getProvincia() { return provincia; }
    public String getCodigoPostal() { return codigoPostal; }

    @Override
    public String toString() {
        return calle + " " + numero + ", " + localidad + ", " + provincia + " (" + codigoPostal + ")";
    }
}
