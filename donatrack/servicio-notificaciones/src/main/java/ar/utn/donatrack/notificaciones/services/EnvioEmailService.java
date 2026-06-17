package ar.utn.donatrack.notificaciones.services;

import org.springframework.stereotype.Service;

@Service
public class EnvioEmailService extends EnvioEstrategia {

    @Override
    protected String getPrefijo() {
        return "EMAIL";
    }
}