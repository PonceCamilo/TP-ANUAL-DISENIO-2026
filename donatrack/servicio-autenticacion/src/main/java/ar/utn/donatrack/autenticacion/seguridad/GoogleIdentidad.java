package ar.utn.donatrack.autenticacion.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class GoogleIdentidad {
    private final JwtDecoder decoder;

    GoogleIdentidad(@Value("${donatrack.auth.google-client-id}") String clientId) {
        if (clientId.isBlank()) {
            decoder = null;
            return;
        }
        var verificador = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
        verificador.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtClaimValidator<String>("iss", iss ->
                        "https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss)),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(clientId)),
                new JwtClaimValidator<String>("azp", azp -> azp == null || clientId.equals(azp)),
                new JwtClaimValidator<Boolean>("email_verified", Boolean.TRUE::equals)));
        decoder = verificador;
    }

    public Jwt verificar(String idToken) {
        if (decoder == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google todavía no está configurado.");
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            if (jwt.getExpiresAt() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || jwt.getClaimAsString("email") == null || jwt.getClaimAsString("email").isBlank()) {
                throw new JwtException("Identidad incompleta.");
            }
            return jwt;
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google no confirmó tu identidad.");
        }
    }
}
