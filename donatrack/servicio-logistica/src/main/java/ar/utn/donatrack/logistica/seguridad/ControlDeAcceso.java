package ar.utn.donatrack.logistica.seguridad;

import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.ParadaResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.exceptions.AccesoDenegadoException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Que una entidad beneficiaria opere solo sobre lo suyo (Entrega 6).
 *
 * El rol lo controla SeguridadConfig; acá se compara el claim perfilId del token
 * (el id de la entidad en Donaciones) con la entidad de la parada, que el broker
 * manda siempre al planificar. Los demás roles pasan: la administración y el
 * chofer operan sobre toda la flota, y al donante lo controla el front contra
 * Donaciones antes de pedir el seguimiento.
 */
@Component
public class ControlDeAcceso {

    /** Una entrega: la entidad tiene que ser la de su parada. */
    public void exigirEntregaVisible(EntregaResponseDTO entrega) {
        if (esEntidad() && !Objects.equals(entrega.getIdEntidadBeneficiaria(), perfilId())) {
            throw new AccesoDenegadoException("Esta entrega no es para tu entidad.");
        }
    }

    /** Una ruta (para seguir el camión): alguna parada tiene que ser de la entidad. */
    public void exigirRutaVisible(RutaResponseDTO ruta) {
        if (!esEntidad()) {
            return;
        }
        UUID perfil = perfilId();
        List<ParadaResponseDTO> paradas = ruta.getParadas() == null ? List.of() : ruta.getParadas();
        boolean pasaPorLaEntidad = perfil != null && paradas.stream()
                .anyMatch(parada -> perfil.equals(parada.getIdEntidadBeneficiaria()));
        if (!pasaPorLaEntidad) {
            throw new AccesoDenegadoException("Esta ruta no pasa por tu entidad.");
        }
    }

    private static boolean esEntidad() {
        Authentication actual = SecurityContextHolder.getContext().getAuthentication();
        return actual != null && actual.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + Roles.ENTIDAD_BENEFICIARIA).equals(a.getAuthority()));
    }

    /** El perfilId del token como UUID, o null si no lo trae (cuenta sin vincular). */
    private static UUID perfilId() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            String perfil = token.getToken().getClaimAsString("perfilId");
            try {
                return perfil == null ? null : UUID.fromString(perfil);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
