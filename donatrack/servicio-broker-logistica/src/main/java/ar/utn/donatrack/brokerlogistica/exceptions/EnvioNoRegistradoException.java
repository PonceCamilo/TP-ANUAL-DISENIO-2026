package ar.utn.donatrack.brokerlogistica.exceptions;

import java.util.UUID;

public class EnvioNoRegistradoException extends RuntimeException {
    public EnvioNoRegistradoException(UUID idDonacion) {
        super("La donación " + idDonacion + " no fue enviada a ningún proveedor de logística");
    }
}
