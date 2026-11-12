package ar.utn.donatrack.incentivos.seguridad;

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

import java.util.List;

@Component
public class AccesoRankingAdmin {
    private static final String BEARER = "Bearer ";
    private final NimbusJwtDecoder verificador;

    public AccesoRankingAdmin(@Value("${servicios.autenticacion.jwk-set-uri:http://localhost:8088/api/autenticacion/claves-publicas}") String claves) {
        verificador = NimbusJwtDecoder.withJwkSetUri(claves).build();
        var audiencia = new JwtClaimValidator<List<String>>(
                "aud", valores -> valores != null && valores.contains("donatrack-api"));
        verificador.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator("urn:donatrack:autenticacion"), audiencia));
    }

    public void exigirAdministrador(String cabecera) {
        if (!cabecera.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere un token.");
        }
        Jwt token;
        try {
            token = verificador.decode(cabecera.substring(BEARER.length()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o vencido.");
        }
        if (!"ADMINISTRADOR".equals(token.getClaimAsString("rol"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere el rol ADMINISTRADOR.");
        }
    }
}
