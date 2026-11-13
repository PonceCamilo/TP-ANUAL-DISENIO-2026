package ar.utn.donatrack.logistica.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autentica a otro servicio de DonaTrack (el broker) por la clave compartida en la
 * cabecera X-DonaTrack-Servicio: le da el rol SERVICIO. La clave se compara en
 * tiempo constante y tiene que tener al menos 32 caracteres; si no está
 * configurada, ningún pedido recibe ese rol.
 *
 * Una clave equivocada no corta el pedido: sigue sin autenticar y termina en 401.
 */
public class ClaveDeServicioFilter extends OncePerRequestFilter {
    public static final String CABECERA = "X-DonaTrack-Servicio";
    static final int LARGO_MINIMO = 32;

    private static final Logger log = LoggerFactory.getLogger(ClaveDeServicioFilter.class);

    private final byte[] clave;

    public ClaveDeServicioFilter(String clave) {
        if (clave == null || clave.isEmpty()) {
            log.warn("No se configuró DONATRACK_SERVICIOS_KEY: el broker no va a poder pedir planificaciones.");
            this.clave = null;
        } else if (clave.length() < LARGO_MINIMO) {
            throw new IllegalStateException("DONATRACK_SERVICIOS_KEY tiene menos de " + LARGO_MINIMO + " caracteres.");
        } else {
            this.clave = clave.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String recibida = pedido.getHeader(CABECERA);
        if (clave != null && recibida != null
                && MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            var servicio = UsernamePasswordAuthenticationToken.authenticated("servicio", null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + Roles.SERVICIO)));
            SecurityContextHolder.getContext().setAuthentication(servicio);
        }
        cadena.doFilter(pedido, respuesta);
    }
}
