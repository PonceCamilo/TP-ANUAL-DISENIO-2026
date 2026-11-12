package ar.utn.donatrack.donaciones.seguridad;

import ar.utn.donatrack.donaciones.exceptions.seguridadExceptions.CuentaConPerfilException;
import ar.utn.donatrack.donaciones.exceptions.seguridadExceptions.CuentaSinPerfilException;
import ar.utn.donatrack.donaciones.exceptions.seguridadExceptions.RecursoAjenoException;
import ar.utn.donatrack.donaciones.util.FiltroDonaciones;
import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * Control de dueño (Entrega 6): además del rol, que ya filtra SeguridadConfig por
 * ruta, cada persona solo opera sobre lo suyo.
 *
 *   - Una persona donante, sobre su propio perfil y sus donaciones.
 *   - Una entidad beneficiaria, sobre su entidad, sus campañas y necesidades, y
 *     las donaciones asignadas a ella.
 *   - La administración, sobre todo.
 *
 * "Lo suyo" lo define el claim perfilId del token, que el Servicio de
 * Autenticación completa cuando la cuenta registra su perfil en Donaciones. Lo
 * usan los controllers antes de llamar al service: si no corresponde, responde
 * 403 (RecursoAjenoException) sin tocar los datos.
 */
@Component
public class ControlDeAcceso {

    private static final Identidad SIN_IDENTIDAD = new Identidad(null, null, null, null);

    /** Quién hace el pedido. Sin token (rutas públicas) no tiene rol: no se le permite nada. */
    public Identidad actual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken token) {
            return Identidad.desde(token.getToken());
        }
        return SIN_IDENTIDAD;
    }

    /** El perfil de una persona donante: la administración o esa misma persona. */
    public void exigirDonante(UUID idDonante) {
        Identidad yo = actual();
        if (yo.esAdministrador()) {
            return;
        }
        if (yo.esDonante()) {
            exigirPerfil(yo, "donante");
            if (yo.esPerfil(idDonante)) {
                return;
            }
        }
        throw new RecursoAjenoException("Solo podés consultar o modificar tu propio perfil de donante.");
    }

    /** Una entidad beneficiaria y lo que cuelga de ella: la administración o esa misma entidad. */
    public void exigirEntidad(UUID idEntidad) {
        Identidad yo = actual();
        if (yo.esAdministrador()) {
            return;
        }
        if (yo.esEntidad()) {
            exigirPerfil(yo, "entidad beneficiaria");
            if (yo.esPerfil(idEntidad)) {
                return;
            }
        }
        throw new RecursoAjenoException("Solo podés gestionar tu propia entidad beneficiaria.");
    }

    /**
     * El detalle de una donación: la administración, la persona que la donó o la
     * entidad a la que se asignó.
     */
    public void exigirVerDonacion(UUID idDonante, UUID idEntidadAsignada) {
        Identidad yo = actual();
        if (yo.esAdministrador()
                || (yo.esDonante() && yo.esPerfil(idDonante))
                || (yo.esEntidad() && idEntidadAsignada != null && yo.esPerfil(idEntidadAsignada))) {
            return;
        }
        throw new RecursoAjenoException("Esta donación no es tuya ni está asignada a tu entidad.");
    }

    /**
     * El listado de donaciones queda limitado a lo de quien consulta: una persona
     * donante ve solo las suyas y una entidad solo las asignadas a ella, aunque no
     * pase el filtro. Pedir las de otro responde 403 en lugar de devolver vacío,
     * para que el error se note. La administración ve todas.
     */
    public FiltroDonaciones limitarAlUsuario(FiltroDonaciones filtro) {
        Identidad yo = actual();
        if (yo.esAdministrador()) {
            return filtro;
        }
        if (yo.esDonante()) {
            exigirPerfil(yo, "donante");
            if (filtro.idDonante() != null && !yo.esPerfil(filtro.idDonante())) {
                throw new RecursoAjenoException("Solo podés ver tus propias donaciones.");
            }
            return filtro.conDonante(yo.perfilId());
        }
        if (yo.esEntidad()) {
            exigirPerfil(yo, "entidad beneficiaria");
            if (filtro.idEntidad() != null && !yo.esPerfil(filtro.idEntidad())) {
                throw new RecursoAjenoException("Solo podés ver las donaciones asignadas a tu entidad.");
            }
            return filtro.conEntidad(yo.perfilId());
        }
        throw new RecursoAjenoException("No tenés permiso para ver donaciones.");
    }

    /**
     * Alta de una persona donante. La administración puede dar de alta a
     * cualquiera (requisito 15). Una cuenta de donante solo puede registrar su
     * propio perfil: con el email de su cuenta, del tipo de su cuenta (humana o
     * jurídica) y una sola vez.
     *
     * @param email email del perfil que se quiere registrar
     * @param tipo  HUMANA o JURIDICA
     */
    public void exigirAltaDeDonante(String email, String tipo) {
        Identidad yo = actual();
        if (yo.esAdministrador()) {
            return;
        }
        if (!yo.esDonante()) {
            throw new RecursoAjenoException("Solo una cuenta de donante puede registrar un perfil de donante.");
        }
        if (yo.tienePerfil()) {
            throw new CuentaConPerfilException("Tu cuenta ya tiene un perfil de donante registrado.");
        }
        if (!esElEmailDeLaCuenta(yo, email)) {
            throw new RecursoAjenoException("El email del perfil tiene que ser el de tu cuenta.");
        }
        if (yo.tipoDonante() != null && !yo.tipoDonante().equals(tipo)) {
            throw new RecursoAjenoException("Tu cuenta es de persona " + nombreDelTipo(yo.tipoDonante())
                    + ": no podés registrar un perfil de persona " + nombreDelTipo(tipo) + ".");
        }
    }

    /**
     * Alta de una entidad beneficiaria. La administración puede dar de alta
     * cualquiera. Una cuenta de entidad solo puede registrar la suya: la entidad
     * tiene que tener el email de la cuenta como contacto o como representante, y
     * se registra una sola vez.
     *
     * @param emailsDeLaEntidad los emails de contacto y de los representantes de la entidad
     */
    public void exigirAltaDeEntidad(Collection<String> emailsDeLaEntidad) {
        Identidad yo = actual();
        if (yo.esAdministrador()) {
            return;
        }
        if (!yo.esEntidad()) {
            throw new RecursoAjenoException("Solo una cuenta de entidad beneficiaria puede registrar una entidad.");
        }
        if (yo.tienePerfil()) {
            throw new CuentaConPerfilException("Tu cuenta ya tiene una entidad beneficiaria registrada.");
        }
        boolean incluyeLaCuenta = emailsDeLaEntidad.stream()
                .filter(Objects::nonNull)
                .anyMatch(email -> esElEmailDeLaCuenta(yo, email));
        if (!incluyeLaCuenta) {
            throw new RecursoAjenoException(
                    "La entidad tiene que tener el email de tu cuenta como contacto o como representante.");
        }
    }

    private static void exigirPerfil(Identidad yo, String queFalta) {
        if (!yo.tienePerfil()) {
            throw new CuentaSinPerfilException(
                    "Tu cuenta todavía no tiene un perfil de " + queFalta + ": completá el registro.");
        }
    }

    private static boolean esElEmailDeLaCuenta(Identidad yo, String email) {
        return yo.email() != null && yo.email().equals(NormalizadorEmail.normalizar(email));
    }

    private static String nombreDelTipo(String tipo) {
        return "JURIDICA".equals(tipo) ? "jurídica" : "humana";
    }
}
