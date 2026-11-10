package ar.utn.donatrack.autenticacion.seguridad;

import ar.utn.donatrack.autenticacion.dtos.response.TokenResponse;
import ar.utn.donatrack.autenticacion.dtos.response.PerfilSesion;
import ar.utn.donatrack.autenticacion.models.Cuenta;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class Tokens {
    static final String EMISOR = "urn:donatrack:autenticacion";
    static final String AUDIENCIA = "donatrack-api";
    private static final Duration VIGENCIA = Duration.ofHours(1);
    private final RSAKey clave;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    Tokens(@Value("${donatrack.auth.clave-privada}") String archivo) throws IOException, GeneralSecurityException {
        RSAPrivateCrtKey privada = clavePrivada(archivo);
        RSAPublicKey publica = clavePublicaDe(privada);
        String idClave = UUID.nameUUIDFromBytes(publica.getEncoded()).toString();
        clave = new RSAKey.Builder(publica).privateKey(privada)
                .keyID(idClave).build();
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(clave)));
        decoder = crearVerificador(publica);
    }

    public TokenResponse emitir(Cuenta cuenta) {
        PerfilSesion perfil = PerfilSesion.crearPerfilPara(cuenta);
        Instant ahora = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(EMISOR).audience(List.of(AUDIENCIA))
                .subject(cuenta.id().toString()).issuedAt(ahora).expiresAt(ahora.plus(VIGENCIA))
                .claim("email", cuenta.email()).claim("nombre", cuenta.nombre()).claim("rol", cuenta.rol().name());
        if (perfil.tipoDonante() != null) {
            claims.claim("tipoDonante", perfil.tipoDonante().name());
        }
        if (perfil.perfilId() != null) {
            claims.claim("perfilId", perfil.perfilId().toString());
        }
        var cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(clave.getKeyID()).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
        return new TokenResponse(token, VIGENCIA.toSeconds(), cuenta.email(), cuenta.nombre(), cuenta.rol(),
                perfil.tipoDonante(), perfil.perfilId(), perfil.camionId());
    }

    public Jwt verificar(String token) {
        return decoder.decode(token);
    }

    public Map<String, Object> clavesPublicas() {
        return new JWKSet(clave.toPublicJWK()).toJSONObject();
    }

    private static RSAPublicKey clavePublicaDe(RSAPrivateCrtKey privada) throws GeneralSecurityException {
        var datosPublicos = new RSAPublicKeySpec(privada.getModulus(), privada.getPublicExponent());
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(datosPublicos);
    }

    private static JwtDecoder crearVerificador(RSAPublicKey publica) {
        var verificador = NimbusJwtDecoder.withPublicKey(publica).build();
        var emisorEsperado = new JwtIssuerValidator(EMISOR);
        var audienciaEsperada = new JwtClaimValidator<List<String>>(
                "aud", audiencias -> audiencias != null && audiencias.contains(AUDIENCIA));
        verificador.setJwtValidator(JwtValidators.createDefaultWithValidators(emisorEsperado, audienciaEsperada));
        return verificador;
    }

    private static RSAPrivateCrtKey clavePrivada(String archivo) throws IOException, GeneralSecurityException {
        if (archivo.isBlank()) {
            return generarClaveDeAutenticacion();
        }
        Path ruta = Path.of(archivo).toAbsolutePath();
        if (!Files.exists(ruta)) {
            Files.createDirectories(ruta.getParent());
            Files.write(ruta, generarClaveDeAutenticacion().getEncoded(), StandardOpenOption.CREATE_NEW);
        }
        return (RSAPrivateCrtKey) KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Files.readAllBytes(ruta)));
    }

    private static RSAPrivateCrtKey generarClaveDeAutenticacion() throws GeneralSecurityException {
        var generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        return (RSAPrivateCrtKey) generador.generateKeyPair().getPrivate();
    }
}
