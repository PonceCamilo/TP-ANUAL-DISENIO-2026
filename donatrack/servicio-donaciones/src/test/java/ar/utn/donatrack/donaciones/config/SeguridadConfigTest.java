package ar.utn.donatrack.donaciones.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La validación del JWT (Entrega 6): firma aparte (la verifica Nimbus con las
 * claves públicas de Autenticación), Donaciones exige emisor, audiencia y
 * vigencia, con los mismos valores que escribe el Servicio de Autenticación.
 */
@DisplayName("SeguridadConfig: validación del token y roles")
class SeguridadConfigTest {

    private static final String EMISOR = "urn:donatrack:autenticacion";
    private static final String AUDIENCIA = "donatrack-api";

    private final OAuth2TokenValidator<Jwt> validador = SeguridadConfig.validadorDeTokens(EMISOR, AUDIENCIA);

    private Jwt.Builder tokenValido() {
        Instant ahora = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(EMISOR)
                .audience(List.of(AUDIENCIA))
                .subject("cuenta-1")
                .issuedAt(ahora.minusSeconds(60))
                .expiresAt(ahora.plusSeconds(3600))
                .claim("rol", "DONANTE");
    }

    @Test
    @DisplayName("Un token de Autenticación, para esta audiencia y vigente, es válido")
    void aceptaUnTokenValido() {
        assertThat(validador.validate(tokenValido().build()).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Se rechaza un token de otro emisor")
    void otroEmisor() {
        assertThat(validador.validate(tokenValido().issuer("https://otro-emisor").build()).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Se rechaza un token emitido para otra audiencia")
    void otraAudiencia() {
        assertThat(validador.validate(tokenValido().audience(List.of("otra-api")).build()).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Se rechaza un token sin audiencia")
    void sinAudiencia() {
        Jwt sinAud = Jwt.withTokenValue("token").header("alg", "RS256").issuer(EMISOR).subject("cuenta-1")
                .issuedAt(Instant.now().minusSeconds(60)).expiresAt(Instant.now().plusSeconds(3600)).build();

        assertThat(validador.validate(sinAud).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Se rechaza un token vencido")
    void vencido() {
        Jwt vencido = tokenValido()
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        assertThat(validador.validate(vencido).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("El claim rol se convierte en la autoridad ROLE_<rol>")
    void rolComoAutoridad() {
        assertThat(SeguridadConfig.autoridadesDelToken().convert(tokenValido().build()))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_DONANTE");
    }
}
