package ar.utn.donatrack.donaciones.seguridad;

import ar.utn.donatrack.donaciones.config.SeguridadConfig;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Tokens para los tests de la capa web (Entrega 6), con los mismos claims que
 * escribe el Servicio de Autenticación: rol, email, perfilId y tipoDonante. El
 * rol pasa a autoridad con la misma conversión que producción
 * (SeguridadConfig.autoridadesDelToken).
 *
 * jwt() no verifica la firma: lo que se prueba con esto son los permisos. La
 * validación del token (emisor, audiencia, vigencia) está en SeguridadConfigTest.
 */
public final class TokensDePrueba {

    public static final String EMAIL_ADMINISTRADOR = "admin@donatrack.org";

    private TokensDePrueba() {
    }

    public static JwtRequestPostProcessor administrador() {
        return token(Roles.ADMINISTRADOR, EMAIL_ADMINISTRADOR, null, null);
    }

    /** Persona humana donante, con su perfil ya registrado (perfilId). */
    public static JwtRequestPostProcessor donante(UUID perfilId, String email) {
        return token(Roles.DONANTE, email, perfilId, "HUMANA");
    }

    /** Cuenta de donante que todavía no registró su perfil en Donaciones. */
    public static JwtRequestPostProcessor donanteSinPerfil(String email, String tipoDonante) {
        return token(Roles.DONANTE, email, null, tipoDonante);
    }

    public static JwtRequestPostProcessor entidad(UUID perfilId, String email) {
        return token(Roles.ENTIDAD_BENEFICIARIA, email, perfilId, null);
    }

    public static JwtRequestPostProcessor entidadSinPerfil(String email) {
        return token(Roles.ENTIDAD_BENEFICIARIA, email, null, null);
    }

    public static JwtRequestPostProcessor chofer() {
        return token(Roles.CHOFER, "chofer@donatrack.org", null, null);
    }

    public static JwtRequestPostProcessor token(String rol, String email, UUID perfilId, String tipoDonante) {
        return jwt()
                .jwt(token -> {
                    token.subject(UUID.randomUUID().toString())
                            .issuer("urn:donatrack:autenticacion")
                            .audience(java.util.List.of("donatrack-api"))
                            .claim("rol", rol)
                            .claim("email", email);
                    if (perfilId != null) {
                        token.claim("perfilId", perfilId.toString());
                    }
                    if (tipoDonante != null) {
                        token.claim("tipoDonante", tipoDonante);
                    }
                })
                .authorities(SeguridadConfig.autoridadesDelToken());
    }
}
