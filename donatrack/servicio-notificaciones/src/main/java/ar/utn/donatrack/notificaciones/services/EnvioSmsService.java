package ar.utn.donatrack.notificaciones.services;

import org.springframework.stereotype.Service;

@Service
public class EnvioSmsService extends EnvioEstrategia {

    @Override
    protected String getPrefijo() {
        return "SMS";
    }
}