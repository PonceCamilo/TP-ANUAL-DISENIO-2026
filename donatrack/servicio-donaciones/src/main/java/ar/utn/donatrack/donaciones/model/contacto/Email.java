package ar.utn.donatrack.donaciones.model.contacto;

/**
 * Medio de contacto por correo electrónico.
 * Es el ÚNICO medio obligatorio para toda persona donante.
 */
public class Email extends MedioDeContacto {

    public Email(String direccion) {
        super(direccion);
    }

    @Override
    public TipoMedioContacto getTipo() {
        return TipoMedioContacto.EMAIL;
    }
}
