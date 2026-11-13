package ar.utn.donatrack.logistica.config;

import ar.utn.donatrack.logistica.seguridad.ClaveDeServicioFilter;
import ar.utn.donatrack.logistica.seguridad.Roles;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seguridad del Servicio de Logística (Entrega 6).
 *
 * Cada pedido trae el JWT del Servicio de Autenticación (lo agrega el servidor del
 * front). Se valida la firma con las claves públicas de Autenticación (RS256), el
 * emisor, la audiencia y la vigencia, y se decide con el claim `rol`:
 *
 *   ADMINISTRADOR    la flota, la planificación, los reintentos y la revisión de
 *                    entregas no recibidas.
 *   CHOFER           su ruta: verla, iniciarla, informar la ubicación del camión y
 *                    marcar una entrega como no recibida.
 *   ENTIDAD_BENEF.   confirmar la recepción de lo que le llega (con fotos) y seguir
 *                    el camión en el mapa. Solo sobre sus entregas: lo controla
 *                    ControlDeAcceso con el claim perfilId contra la parada.
 *   DONANTE          seguir en el mapa la entrega de su donación. Logística no sabe
 *                    quién donó: el front confirma antes con Donaciones que la
 *                    donación es de esa persona.
 *   SERVICIO         el broker, con la clave entre servicios (X-DonaTrack-Servicio):
 *                    pide la planificación de las donaciones asignadas.
 *
 * Dos puertas no usan el JWT, a propósito, y tienen su propia cadena:
 * - El callback de planificación es para un proveedor externo de ruteo que
 *   responde más tarde. Un tercero no tiene token de DonaTrack: se autentica con el
 *   token de correlación del lote, que viaja en Authorization y valida
 *   PlanificacionRutasService. Si esa cabecera pasara por la validación de JWT,
 *   el callback se rechazaría siempre.
 * - /ruteo/planificar simula a ese proveedor; Logística se lo pide a sí misma.
 *
 * Sin sesión ni cookies, así que tampoco CSRF. Los 401 y 403 salen con el mismo
 * formato que el resto de los errores del servicio (GlobalExceptionHandler).
 */
@Configuration
@EnableWebSecurity
// Los tests de persistencia levantan el contexto sin capa web: ahí no hay filtros.
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SeguridadConfig {

    static final String SIN_TOKEN = "Falta el token de acceso o no es válido. Iniciá sesión de nuevo.";
    static final String SIN_PERMISO = "No tenés permiso para hacer esta operación.";

    private static final String ADMINISTRADOR = Roles.ADMINISTRADOR;
    private static final String CHOFER = Roles.CHOFER;
    private static final String ENTIDAD = Roles.ENTIDAD_BENEFICIARIA;
    private static final String DONANTE = Roles.DONANTE;
    private static final String SERVICIO = Roles.SERVICIO;

    private static final String CAMIONES = "/api/logistica/camiones";
    private static final String ENTREGAS = "/api/logistica/entregas";
    private static final String RUTAS = "/api/logistica/rutas";
    private static final String PLANIFICACIONES = "/api/logistica/planificaciones";

    /** Callback del proveedor de ruteo y proveedor simulado: tienen su propia autenticación (o ninguna). */
    @Bean
    @Order(1)
    public SecurityFilterChain integracionesDeRuteo(HttpSecurity http) throws Exception {
        http.securityMatcher(PLANIFICACIONES + "/callback", "/ruteo/**")
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ObjectMapper json,
                                                 @Value("${logistica.seguridad.clave-servicios:}") String claveServicios)
            throws Exception {
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
                .addFilterBefore(new ClaveDeServicioFilter(claveServicios), BearerTokenAuthenticationFilter.class)
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

    /** Las claves públicas se piden a Autenticación con el primer token: Logística arranca aunque no esté. */
    @Bean
    public JwtDecoder decodificadorDeTokens(@Value("${logistica.seguridad.claves-publicas-url}") String clavesPublicas,
                                            @Value("${logistica.seguridad.emisor}") String emisor,
                                            @Value("${logistica.seguridad.audiencia}") String audiencia) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(clavesPublicas)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decodificador.setJwtValidator(validadorDeTokens(emisor, audiencia));
        return decodificador;
    }

    static OAuth2TokenValidator<Jwt> validadorDeTokens(String emisor, String audiencia) {
        OAuth2TokenValidator<Jwt> audienciaEsperada = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, audiencias -> audiencias != null && audiencias.contains(audiencia));
        return JwtValidators.createDefaultWithValidators(new JwtIssuerValidator(emisor), audienciaEsperada);
    }

    /** El claim `rol` se convierte en la autoridad ROLE_<rol>. Público para que los tests usen la misma conversión. */
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

    /** Gana la primera regla que coincide: por eso las rutas más largas van antes. */
    private static void permisosPorRuta(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry reglas) {
        reglas
                // ── Sin token ───────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/error").permitAll()

                // ── Flota ───────────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, CAMIONES + "/{id}/ubicacion").hasAnyRole(CHOFER, ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, CAMIONES + "/{id}/ruta").hasAnyRole(CHOFER, ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, CAMIONES + "/{id}").hasAnyRole(ADMINISTRADOR, CHOFER, ENTIDAD, DONANTE)
                .requestMatchers(CAMIONES, CAMIONES + "/**").hasRole(ADMINISTRADOR)

                // ── Rutas ───────────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, RUTAS + "/{id}/iniciar").hasAnyRole(CHOFER, ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, RUTAS + "/{id}").hasAnyRole(ADMINISTRADOR, CHOFER, ENTIDAD, DONANTE)

                // ── Entregas ────────────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, ENTREGAS + "/{id}/confirmar").hasRole(ENTIDAD)
                .requestMatchers(HttpMethod.POST, ENTREGAS + "/{id}/no-recibida").hasAnyRole(CHOFER, ADMINISTRADOR)
                .requestMatchers(HttpMethod.POST, ENTREGAS + "/{id}/regreso-deposito").hasRole(ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, ENTREGAS + "/por-donacion/{idDonacion}", ENTREGAS + "/{id}")
                        .hasAnyRole(ADMINISTRADOR, CHOFER, ENTIDAD, DONANTE)
                .requestMatchers(HttpMethod.GET, ENTREGAS).hasRole(ADMINISTRADOR)

                // ── Planificación ───────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, PLANIFICACIONES).hasAnyRole(ADMINISTRADOR, SERVICIO)

                // ── Todo lo demás: solo la administración ───────────────────────
                .anyRequest().hasRole(ADMINISTRADOR);
    }

    private static void escribir(HttpServletResponse respuesta, ObjectMapper json, HttpStatus status, String mensaje)
            throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", status.getReasonPhrase());
        cuerpo.put("message", mensaje);
        respuesta.setStatus(status.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(respuesta.getOutputStream(), cuerpo);
    }
}
