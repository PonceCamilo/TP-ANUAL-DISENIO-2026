package ar.utn.donatrack.logistica.seguridad;

import ar.utn.donatrack.logistica.config.SeguridadConfig;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Tokens como los que emite Autenticación, con la misma conversión de rol a autoridad que SeguridadConfig. */
public final class TokensDePrueba {

    private TokensDePrueba() {}

    public static RequestPostProcessor administrador() {
        return token(Roles.ADMINISTRADOR, null);
    }

    public static RequestPostProcessor chofer() {
        return token(Roles.CHOFER, null);
    }

    public static RequestPostProcessor donante() {
        return token(Roles.DONANTE, UUID.randomUUID());
    }

    public static RequestPostProcessor entidad(UUID perfilId) {
        return token(Roles.ENTIDAD_BENEFICIARIA, perfilId);
    }

    private static RequestPostProcessor token(String rol, UUID perfilId) {
        return jwt().jwt(datos -> {
            datos.subject(UUID.randomUUID().toString()).claim("rol", rol);
            if (perfilId != null) {
                datos.claim("perfilId", perfilId.toString());
            }
        }).authorities(SeguridadConfig.autoridadesDelToken());
    }
}
