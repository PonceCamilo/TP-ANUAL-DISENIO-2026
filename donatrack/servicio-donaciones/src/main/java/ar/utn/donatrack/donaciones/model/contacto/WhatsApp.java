package ar.utn.donatrack.donaciones.model.contacto;

/** Medio de contacto por WhatsApp. Opcional. */
public class WhatsApp extends MedioDeContacto {

    public WhatsApp(String numero) {
        super(numero);
    }

    @Override
    public TipoMedioContacto getTipo() {
        return TipoMedioContacto.WHATSAPP;
    }
}
