package ar.utn.donatrack.notificaciones.services;

import org.springframework.stereotype.Service;

@Service
public class EnvioWhatsappService extends EnvioEstrategia {

    @Override
    protected String getPrefijo() {
        return "WHATSAPP";
    }
}
