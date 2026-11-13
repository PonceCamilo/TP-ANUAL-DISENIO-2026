package ar.utn.donatrack.notificaciones.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Quién ve qué notificaciones (Entrega 6).
 *
 * Antes GET /notificaciones devolvía las de todos, sin login. Ahora:
 *
 * - Leer o marcar como leídas pide el JWT de Autenticación.
 * - La administración ve todas, o las de los destinatarios que pida.
 * - Cualquier otra persona ve las que le llegaron a su email (el del token).
 *   Las de sus otros medios de contacto (por ejemplo, su WhatsApp) las puede pedir
 *   solo el front, con su clave: los saca del perfil de Donaciones con el token de
 *   esa persona, y Donaciones solo le da el perfil propio.
 * - Enviar por HTTP (POST) es solo de los servicios de DonaTrack, con la clave
 *   entre servicios. Por RabbitMQ no cambia nada.
 */
@Component
public class ControlDeAcceso {
    public static final String CABECERA_FRONTEND = "X-DonaTrack-Frontend";
    public static final String CABECERA_SERVICIO = "X-DonaTrack-Servicio";
    private static final String BEARER = "Bearer ";
    private static final int LARGO_MINIMO_CLAVE = 32;
    private static final Logger log = LoggerFactory.getLogger(ControlDeAcceso.class);

    private final NimbusJwtDecoder verificador;
    private final byte[] claveFrontend;
    private final byte[] claveServicios;

    public ControlDeAcceso(@Value("${servicios.autenticacion.jwk-set-uri:http://localhost:8088/api/autenticacion/claves-publicas}") String claves,
                           @Value("${notificaciones.seguridad.clave-frontend:}") String claveFrontend,
                           @Value("${notificaciones.seguridad.clave-servicios:}") String claveServicios) {
        verificador = NimbusJwtDecoder.withJwkSetUri(claves).build();
        var audiencia = new JwtClaimValidator<List<String>>(
                "aud", valores -> valores != null && valores.contains("donatrack-api"));
        verificador.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator("urn:donatrack:autenticacion"), audiencia));
        this.claveFrontend = clave(claveFrontend, "AUTH_FRONTEND_KEY",
                "solo se mostrarán las notificaciones que llegaron al email de cada persona.");
        this.claveServicios = clave(claveServicios, "DONATRACK_SERVICIOS_KEY",
                "se rechazarán los avisos por HTTP (por RabbitMQ siguen llegando).");
    }

    /** Quién pide: la administración o una persona con su email. */
    public Lector lector(String cabecera) {
        Jwt token = verificar(cabecera);
        String email = token.getClaimAsString("email");
        return new Lector("ADMINISTRADOR".equals(token.getClaimAsString("rol")),
                email == null ? "" : email.strip().toLowerCase(Locale.ROOT));
    }

    /**
     * Los destinatarios que puede consultar o marcar como leídos. Para la
     * administración, los que pidió (null es "todos"). Para las demás personas,
     * su email; los otros los acepta solo si los pide el front con su clave.
     */
    public List<String> destinatariosPermitidos(Lector lector, String claveDelFront, List<String> pedidos) {
        if (lector.administrador()) {
            return pedidos == null || pedidos.isEmpty() ? null : pedidos;
        }
        List<String> permitidos = new ArrayList<>();
        permitidos.add(lector.email());
        if (pedidos == null) {
            return permitidos;
        }
        boolean desdeElFront = coincide(claveFrontend, claveDelFront);
        for (String pedido : pedidos) {
            if (pedido == null || pedido.isBlank() || pedido.strip().equalsIgnoreCase(lector.email())) {
                continue;
            }
            if (!desdeElFront) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo podés ver las notificaciones que te llegaron a vos.");
            }
            permitidos.add(pedido.strip());
        }
        return permitidos;
    }

    /** Una notificación puntual: la administración o quien la recibió en su email. */
    public void exigirPropia(Lector lector, String destinatario) {
        if (!lector.administrador() && (destinatario == null || !destinatario.strip().equalsIgnoreCase(lector.email()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo podés ver las notificaciones que te llegaron a vos.");
        }
    }

    public void exigirServicio(String clave) {
        if (!coincide(claveServicios, clave)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los servicios de DonaTrack pueden enviar notificaciones.");
        }
    }

    private Jwt verificar(String cabecera) {
        if (cabecera == null || !cabecera.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere un token.");
        }
        try {
            return verificador.decode(cabecera.substring(BEARER.length()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o vencido.");
        }
    }

    private static byte[] clave(String valor, String variable, String sinClave) {
        if (valor == null || valor.isEmpty()) {
            log.warn("No se configuró {}: {}", variable, sinClave);
            return null;
        }
        if (valor.length() < LARGO_MINIMO_CLAVE) {
            throw new IllegalStateException(variable + " tiene menos de " + LARGO_MINIMO_CLAVE + " caracteres.");
        }
        return valor.getBytes(StandardCharsets.UTF_8);
    }

    private static boolean coincide(byte[] esperada, String recibida) {
        return esperada != null && recibida != null
                && MessageDigest.isEqual(esperada, recibida.getBytes(StandardCharsets.UTF_8));
    }

    /** Quién lee: si es de la administración y su email (normalizado). */
    public record Lector(boolean administrador, String email) {}
}
