package ar.utn.donatrack.autenticacion.services;

import ar.utn.donatrack.autenticacion.correo.CorreoDeActivacion;
import ar.utn.donatrack.autenticacion.dtos.response.TokenResponse;
import ar.utn.donatrack.autenticacion.models.Activacion;
import ar.utn.donatrack.autenticacion.models.Cuenta;
import ar.utn.donatrack.autenticacion.models.Rol;
import ar.utn.donatrack.autenticacion.repositories.Activaciones;
import ar.utn.donatrack.autenticacion.repositories.Cuentas;
import ar.utn.donatrack.autenticacion.seguridad.Contrasenias;
import ar.utn.donatrack.autenticacion.seguridad.Tokens;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Enlaces de activación (Entrega 6, requisito 20).
 *
 * Las personas que cargó la administración tienen cuenta pero no contraseña: piden
 * el enlace con su email, lo reciben por correo y eligen la contraseña. El mismo
 * camino sirve para recuperar el acceso a una cuenta de donante o de entidad.
 *
 * - Pedir un enlace responde siempre lo mismo, exista o no la cuenta: no revela qué
 *   emails están registrados.
 * - El código es aleatorio (256 bits) y se guarda solo su hash.
 * - Vence (donatrack.auth.activacion.vigencia), sirve una vez y pedir otro borra
 *   los anteriores. Entre dos pedidos de la misma cuenta tiene que pasar un minuto.
 */
@Service
public class ActivacionService {
    static final Duration ESPERA_ENTRE_PEDIDOS = Duration.ofMinutes(1);
    private static final SecureRandom AZAR = new SecureRandom();

    private final Cuentas cuentas;
    private final Activaciones activaciones;
    private final Contrasenias claves;
    private final Tokens tokens;
    private final CorreoDeActivacion correo;
    private final String frontendUrl;
    private final Duration vigencia;

    ActivacionService(Cuentas cuentas, Activaciones activaciones, Contrasenias claves, Tokens tokens,
                      CorreoDeActivacion correo,
                      @Value("${donatrack.auth.frontend-url:http://localhost:8080}") String frontendUrl,
                      @Value("${donatrack.auth.activacion.vigencia:PT24H}") Duration vigencia) {
        this.cuentas = cuentas;
        this.activaciones = activaciones;
        this.claves = claves;
        this.tokens = tokens;
        this.correo = correo;
        this.frontendUrl = frontendUrl;
        this.vigencia = vigencia;
    }

    @Transactional
    public void solicitar(String email, Rol rol) {
        if (!rol.esDonante()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo las cuentas de donantes y de entidades se activan por email.");
        }
        Cuenta cuenta = cuentas.findByEmailAndRol(email.strip().toLowerCase(Locale.ROOT), rol).orElse(null);
        if (cuenta == null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        boolean pedidoReciente = activaciones.findFirstByCuentaOrderByCreadaEnDesc(cuenta)
                .filter(ultima -> !ultima.usada() && ultima.creadaEn().isAfter(ahora.minus(ESPERA_ENTRE_PEDIDOS)))
                .isPresent();
        if (pedidoReciente) {
            return;
        }
        activaciones.borrarPendientes(cuenta);
        String codigo = nuevoCodigo();
        activaciones.save(new Activacion(cuenta, hash(codigo), ahora, ahora.plus(vigencia)));
        correo.enviar(cuenta.email(), cuenta.nombre(), enlace(codigo), cuenta.pendienteDeActivacion(), vigencia);
    }

    @Transactional
    public TokenResponse confirmar(String codigo, String password) {
        String hash = claves.codificar(password);
        Activacion activacion = activaciones.findByCodigoHash(hash(codigo)).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El enlace no es válido o fue reemplazado por uno más nuevo. Pedí otro."));
        LocalDateTime ahora = LocalDateTime.now();
        if (activacion.usada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este enlace ya se usó. Si no recordás la contraseña, pedí otro.");
        }
        if (activacion.vencida(ahora)) {
            throw new ResponseStatusException(HttpStatus.GONE, "El enlace venció. Pedí otro.");
        }
        Cuenta cuenta = activacion.cuenta();
        cuenta.definirClave(hash);
        activacion.marcarUsada(ahora);
        return tokens.emitir(cuentas.saveAndFlush(cuenta));
    }

    private String enlace(String codigo) {
        return UriComponentsBuilder.fromUriString(frontendUrl).path("/activar")
                .queryParam("codigo", codigo).build().toUriString();
    }

    private static String nuevoCodigo() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String codigo) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256.", e);
        }
    }
}
