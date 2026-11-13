package ar.utn.donatrack.autenticacion.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Claves que comparten los servidores de DonaTrack para las operaciones que no
 * hace una persona con su token:
 *
 * - Clave del frontend (AUTH_FRONTEND_KEY, cabecera X-DonaTrack-Frontend): vincular
 *   el perfil de Donaciones y los enlaces de activación. Es obligatoria: sin ella el
 *   front no puede completar un registro, así que el servicio no arranca y lo dice,
 *   en lugar de fallar recién cuando alguien se registra.
 * - Clave entre servicios (DONATRACK_SERVICIOS_KEY, cabecera X-DonaTrack-Servicio):
 *   el aviso de cuentas importadas que manda Donaciones. Es opcional: sin ella se
 *   rechazan esos avisos.
 *
 * Las dos se comparan en tiempo constante y piden al menos 32 caracteres.
 */
@Component
public class ClavesCompartidas {
    public static final String CABECERA_FRONTEND = "X-DonaTrack-Frontend";
    public static final String CABECERA_SERVICIO = "X-DonaTrack-Servicio";
    static final int LARGO_MINIMO = 32;

    private static final Logger log = LoggerFactory.getLogger(ClavesCompartidas.class);

    private final byte[] frontend;
    private final byte[] servicios;

    ClavesCompartidas(@Value("${donatrack.auth.clave-frontend}") String claveFrontend,
                      @Value("${donatrack.auth.clave-servicios:}") String claveServicios) {
        if (claveFrontend == null || claveFrontend.length() < LARGO_MINIMO) {
            throw new IllegalStateException("Falta AUTH_FRONTEND_KEY o tiene menos de " + LARGO_MINIMO
                    + " caracteres. Configurá la misma clave en Autenticación (donatrack/.env) y en el frontend.");
        }
        if (claveServicios != null && !claveServicios.isEmpty() && claveServicios.length() < LARGO_MINIMO) {
            throw new IllegalStateException("DONATRACK_SERVICIOS_KEY tiene menos de " + LARGO_MINIMO + " caracteres.");
        }
        this.frontend = claveFrontend.getBytes(StandardCharsets.UTF_8);
        this.servicios = claveServicios == null || claveServicios.isEmpty()
                ? null : claveServicios.getBytes(StandardCharsets.UTF_8);
        if (this.servicios == null) {
            log.warn("No se configuró DONATRACK_SERVICIOS_KEY: se rechazarán los avisos de cuentas importadas de Donaciones.");
        }
    }

    public void exigirFrontend(String clave, String motivo) {
        if (!coincide(frontend, clave)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, motivo);
        }
    }

    public void exigirServicio(String clave) {
        if (servicios == null || !coincide(servicios, clave)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta operación solo la pueden pedir los servicios de DonaTrack.");
        }
    }

    private static boolean coincide(byte[] esperada, String recibida) {
        return recibida != null && MessageDigest.isEqual(esperada, recibida.getBytes(StandardCharsets.UTF_8));
    }
}
