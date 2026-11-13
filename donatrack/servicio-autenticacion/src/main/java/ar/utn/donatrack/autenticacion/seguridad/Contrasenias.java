package ar.utn.donatrack.autenticacion.seguridad;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/**
 * Reglas y hash de las contraseñas (BCrypt), compartidas por el registro y la
 * activación. BCrypt solo mira los primeros 72 bytes: por eso es el máximo.
 */
@Component
public class Contrasenias {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public String codificar(String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres y hasta 72 bytes.");
        }
        return bcrypt.encode(password);
    }

    public boolean coincide(String password, String hash) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72 && bcrypt.matches(password, hash);
    }
}
