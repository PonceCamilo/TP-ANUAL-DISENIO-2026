package ar.utn.donatrack.donaciones.config;

import ar.utn.donatrack.donaciones.dtos.response.ErrorResponseDTO;
import ar.utn.donatrack.donaciones.seguridad.Roles;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguridad del Servicio de Donaciones (Entrega 6).
 *
 * Cada pedido trae en Authorization: Bearer el JWT que emite el Servicio de
 * Autenticación (lo agrega el servidor del front). Donaciones no maneja usuarios
 * ni contraseñas: verifica la firma con las claves públicas de Autenticación
 * (RS256), que el token sea de ese emisor y para esta audiencia, y que no esté
 * vencido. Después decide con el claim `rol`.
 *
 * Qué puede hacer cada rol, por ruta (lo que no figura es solo de la administración):
 *
 *   Sin token        health e info de Actuator, Swagger, los datos públicos de la
 *                    landing (GET /publico/**, sin datos personales) y los avisos
 *                    del broker de logística (/logistica/eventos/**): el broker
 *                    todavía no manda token, eso queda para la Entrega 7.
 *   DONANTE          registrar su perfil, verlo y modificarlo; registrar donaciones
 *                    y ver las suyas.
 *   ENTIDAD_BENEF.   registrar su entidad, verla y modificarla, con sus campañas y
 *                    necesidades; ver las donaciones asignadas a ella.
 *   ADMINISTRADOR    todo: listados completos, búsquedas por email, cambios de
 *                    estado, asignación, despacho, bajas e importación por CSV.
 *
 * El rol se controla acá; que cada uno opere solo sobre lo suyo (su perfil, sus
 * donaciones) lo controla ControlDeAcceso con el claim perfilId.
 *
 * No hay sesión ni cookies (cada pedido se valida solo), así que tampoco CSRF.
 * Los 401 y 403 salen con el mismo ErrorResponseDTO que el resto de los errores.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(DonacionesProperties.class)
public class SeguridadConfig {

    static final String SIN_TOKEN = "Falta el token de acceso o no es válido. Iniciá sesión de nuevo.";
    static final String SIN_PERMISO = "No tenés permiso para hacer esta operación.";

    private static final String ADMINISTRADOR = Roles.ADMINISTRADOR;
    private static final String DONANTE = Roles.DONANTE;
    private static final String ENTIDAD = Roles.ENTIDAD_BENEFICIARIA;

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ObjectMapper json) throws Exception {
        AuthenticationEntryPoint sinToken = (pedido, respuesta, error) -> {
            respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escribir(respuesta, json, HttpStatus.UNAUTHORIZED, SIN_TOKEN);
        };
        AccessDeniedHandler sinPermiso = (pedido, respuesta, error) ->
                escribir(respuesta, json, HttpStatus.FORBIDDEN, SIN_PERMISO);

        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(SeguridadConfig::permisosPorRuta)
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesDelToken()))
                        .authenticationEntryPoint(sinToken)
                        .accessDeniedHandler(sinPermiso))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(sinToken)
                        .accessDeniedHandler(sinPermiso));
        return http.build();
    }

    /**
     * Valida firma, emisor, audiencia y vigencia. Las claves públicas se piden a
     * Autenticación con el primer token y quedan en memoria: Donaciones arranca
     * aunque ese servicio todavía no esté arriba.
     */
    @Bean
    public JwtDecoder decodificadorDeTokens(DonacionesProperties propiedades) {
        DonacionesProperties.Seguridad seguridad = propiedades.seguridad();
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(seguridad.clavesPublicasUrl())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decodificador.setJwtValidator(validadorDeTokens(seguridad.emisor(), seguridad.audiencia()));
        return decodificador;
    }

    /** Vigencia (exp y nbf), emisor (iss) y audiencia (aud): los mismos que usa Autenticación. */
    static OAuth2TokenValidator<Jwt> validadorDeTokens(String emisor, String audiencia) {
        OAuth2TokenValidator<Jwt> audienciaEsperada = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, audiencias -> audiencias != null && audiencias.contains(audiencia));
        return JwtValidators.createDefaultWithValidators(new JwtIssuerValidator(emisor), audienciaEsperada);
    }

    /**
     * El claim `rol` (un solo valor, por ejemplo DONANTE) se convierte en la
     * autoridad ROLE_DONANTE. Es público para que los tests armen sus tokens con
     * la misma conversión.
     */
    public static JwtGrantedAuthoritiesConverter autoridadesDelToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");
        return roles;
    }

    static JwtAuthenticationConverter rolesDelToken() {
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridadesDelToken());
        return conversor;
    }

    /**
     * Las reglas se evalúan en orden y gana la primera que coincide: por eso
     * /donaciones/candidatos va antes que /donaciones/{id}.
     */
    private static void permisosPorRuta(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry reglas) {
        reglas
                // ── Sin token ───────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                .requestMatchers("/logistica/eventos/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/publico/**").permitAll()
                .requestMatchers("/error").permitAll()

                // ── Personas donantes ───────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/donantes").hasAnyRole(ADMINISTRADOR, DONANTE)
                .requestMatchers(HttpMethod.GET, "/donantes/{id}").hasAnyRole(ADMINISTRADOR, DONANTE)
                .requestMatchers(HttpMethod.PUT, "/donantes/{id}").hasAnyRole(ADMINISTRADOR, DONANTE)
                .requestMatchers(HttpMethod.PATCH, "/donantes/{id}/contactos", "/donantes/{id}/representantes")
                        .hasAnyRole(ADMINISTRADOR, DONANTE)

                // ── Entidades beneficiarias ─────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/entidades").hasAnyRole(ADMINISTRADOR, ENTIDAD)
                .requestMatchers(HttpMethod.GET, "/entidades/{id}", "/entidades/{id}/campanias/**")
                        .hasAnyRole(ADMINISTRADOR, ENTIDAD)
                .requestMatchers(HttpMethod.PUT, "/entidades/{id}", "/entidades/{id}/campanias/**")
                        .hasAnyRole(ADMINISTRADOR, ENTIDAD)
                .requestMatchers(HttpMethod.POST, "/entidades/{id}/campanias", "/entidades/{id}/campanias/**")
                        .hasAnyRole(ADMINISTRADOR, ENTIDAD)
                .requestMatchers(HttpMethod.DELETE, "/entidades/{id}/campanias/**").hasAnyRole(ADMINISTRADOR, ENTIDAD)

                // ── Donaciones ──────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/donaciones/candidatos").hasRole(ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, "/donaciones", "/donaciones/{id}")
                        .hasAnyRole(ADMINISTRADOR, DONANTE, ENTIDAD)
                .requestMatchers(HttpMethod.POST, "/donaciones").hasAnyRole(ADMINISTRADOR, DONANTE)

                // ── Todo lo demás: solo la administración ───────────────────────
                .anyRequest().hasRole(ADMINISTRADOR);
    }

    private static void escribir(HttpServletResponse respuesta, ObjectMapper json, HttpStatus status, String mensaje)
            throws IOException {
        respuesta.setStatus(status.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(respuesta.getOutputStream(), ErrorResponseDTO.de(status, mensaje));
    }
}
