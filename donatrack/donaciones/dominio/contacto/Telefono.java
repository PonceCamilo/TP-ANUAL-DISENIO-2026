package com.donatrack.donaciones.dominio.contacto;

/** Medio de contacto por teléfono. Opcional. */
public class Telefono extends MedioDeContacto {

    public Telefono(String numero) {
        super(numero);
    }

    @Override
    public TipoMedioContacto getTipo() {
        return TipoMedioContacto.TELEFONO;
    }
}
