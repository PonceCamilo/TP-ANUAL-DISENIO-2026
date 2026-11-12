package ar.utn.donatrack.donaciones.seguridad;

import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Quién hace el pedido, según el JWT del Servicio de Autenticación (Entrega 6).
 *
 * Claims que usa Donaciones:
 *   - rol: DONANTE, ENTIDAD_BENEFICIARIA, ADMINISTRADOR o CHOFER (ver Roles).
 *   - email: el de la cuenta. Se guarda normalizado igual que los emails de
 *     Donaciones (NormalizadorEmail), para poder compararlos.
 *   - perfilId: el id de la persona donante o de la entidad en Donaciones. No
 *     viene hasta que la cuenta completa su registro.
 *   - tipoDonante: HUMANA o JURIDICA, solo en las cuentas de donantes.
 *
 * @param email       email de la cuenta, normalizado
 * @param rol         rol de la cuenta; null si el token no lo trae
 * @param perfilId    id del perfil en Donaciones; null si todavía no tiene
 * @param tipoDonante HUMANA o JURIDICA; null si no es una cuenta de donante
 */
public record Identidad(String email, String rol, UUID perfilId, String tipoDonante) {

    public static Identidad desde(Jwt token) {
        String perfil = token.getClaimAsString("perfilId");
        return new Identidad(
                NormalizadorEmail.normalizar(token.getClaimAsString("email")),
                token.getClaimAsString("rol"),
                perfil == null || perfil.isBlank() ? null : UUID.fromString(perfil),
                token.getClaimAsString("tipoDonante"));
    }

    public boolean esAdministrador() {
        return Roles.ADMINISTRADOR.equals(rol);
    }

    public boolean esDonante() {
        return Roles.DONANTE.equals(rol);
    }

    public boolean esEntidad() {
        return Roles.ENTIDAD_BENEFICIARIA.equals(rol);
    }

    public boolean tienePerfil() {
        return perfilId != null;
    }

    /** Si el perfil del token es el pedido. */
    public boolean esPerfil(UUID id) {
        return perfilId != null && perfilId.equals(id);
    }
}
