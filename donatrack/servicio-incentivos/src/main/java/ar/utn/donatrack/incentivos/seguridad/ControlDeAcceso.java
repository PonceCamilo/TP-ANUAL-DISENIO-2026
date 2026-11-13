package ar.utn.donatrack.incentivos.seguridad;

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
import java.util.List;
import java.util.UUID;

/**
 * Quién puede hacer qué en Incentivos (Entrega 6).
 *
 * Valida el JWT del Servicio de Autenticación (firma con sus claves públicas,
 * emisor, audiencia y vigencia) y decide con los claims rol y perfilId:
 *
 * - Ranking mensual e historial: solo la administración.
 * - Métricas, misiones e insignias de una persona: esa persona (perfilId igual al
 *   id del donante) o la administración.
 * - Mostrar u ocultar una insignia: solo esa persona.
 * - Avisos de donación registrada y exitosa: solo Donaciones, con la clave entre
 *   servicios (X-DonaTrack-Servicio); sin clave configurada se rechazan.
 *
 * El podio y el perfil público siguen sin login: muestran solo lo que la persona
 * eligió mostrar.
 */
@Component
public class ControlDeAcceso {
    public static final String CABECERA_SERVICIO = "X-DonaTrack-Servicio";
    private static final String BEARER = "Bearer ";
    private static final int LARGO_MINIMO_CLAVE = 32;
    private static final Logger log = LoggerFactory.getLogger(ControlDeAcceso.class);

    private final NimbusJwtDecoder verificador;
    private final byte[] claveServicios;

    public ControlDeAcceso(@Value("${servicios.autenticacion.jwk-set-uri:http://localhost:8088/api/autenticacion/claves-publicas}") String claves,
                           @Value("${servicios.clave-servicios:}") String claveServicios) {
        verificador = NimbusJwtDecoder.withJwkSetUri(claves).build();
        var audiencia = new JwtClaimValidator<List<String>>(
                "aud", valores -> valores != null && valores.contains("donatrack-api"));
        verificador.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator("urn:donatrack:autenticacion"), audiencia));
        if (claveServicios == null || claveServicios.isEmpty()) {
            log.warn("No se configuró DONATRACK_SERVICIOS_KEY: se rechazarán los avisos de donaciones.");
            this.claveServicios = null;
        } else if (claveServicios.length() < LARGO_MINIMO_CLAVE) {
            throw new IllegalStateException("DONATRACK_SERVICIOS_KEY tiene menos de " + LARGO_MINIMO_CLAVE + " caracteres.");
        } else {
            this.claveServicios = claveServicios.getBytes(StandardCharsets.UTF_8);
        }
    }

    public void exigirAdministrador(String cabecera) {
        if (!esAdministrador(verificar(cabecera))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere el rol ADMINISTRADOR.");
        }
    }

    /** Los datos de una persona donante: ella misma o la administración. */
    public void exigirDuenioOAdministracion(String cabecera, UUID donanteId) {
        Jwt token = verificar(cabecera);
        if (!esAdministrador(token) && !esLaPersona(token, donanteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo la persona donante o la administración ven estos datos.");
        }
    }

    /** Lo que solo decide la persona (por ejemplo, qué insignias muestra). */
    public void exigirDuenio(String cabecera, UUID donanteId) {
        if (!esLaPersona(verificar(cabecera), donanteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo la persona donante puede cambiar esto.");
        }
    }

    /** Avisos de otro servicio de DonaTrack (Donaciones), con la clave compartida. */
    public void exigirServicio(String clave) {
        if (claveServicios == null || clave == null
                || !MessageDigest.isEqual(claveServicios, clave.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este aviso solo lo pueden mandar los servicios de DonaTrack.");
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

    private static boolean esAdministrador(Jwt token) {
        return "ADMINISTRADOR".equals(token.getClaimAsString("rol"));
    }

    private static boolean esLaPersona(Jwt token, UUID donanteId) {
        return "DONANTE".equals(token.getClaimAsString("rol")) && donanteId != null
                && donanteId.toString().equals(token.getClaimAsString("perfilId"));
    }
}
