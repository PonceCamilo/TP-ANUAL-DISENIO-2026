package ar.utn.donatrack.autenticacion;

import ar.utn.donatrack.autenticacion.correo.CorreoDeActivacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("Enlaces de activación (requisito 20)")
class ActivacionTest {
    static final String CLAVE_FRONTEND = "clave-solo-para-la-prueba-de-integracion";

    @Autowired MockMvc http;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean CorreoDeActivacion correo;

    @Test
    @DisplayName("La persona importada pide el enlace, elige su contraseña y entra con su perfil")
    void activarUnaCuentaImportada() throws Exception {
        UUID perfil = importar("activa@example.test");

        pedir("Activa@Example.test").andExpect(status().isAccepted());
        String codigo = codigoEnviadoA("activa@example.test", true);

        confirmar(codigo, "mi-contraseña-2026").andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("DONANTE"))
                .andExpect(jsonPath("$.perfilId").value(perfil.toString()));
        http.perform(post("/api/autenticacion/ingreso").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", "activa@example.test",
                                "password", "mi-contraseña-2026", "rol", "DONANTE"))))
                .andExpect(status().isOk());

        confirmar(codigo, "otra-contraseña-2026").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Pedir el enlace responde igual exista o no la cuenta, y solo manda correo si existe")
    void noRevelaQueEmailsExisten() throws Exception {
        pedir("nadie@example.test").andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Si hay una cuenta con ese email, te mandamos un enlace para elegir tu contraseña."));
        verify(correo, never()).enviar(eq("nadie@example.test"), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("Solo el frontend con su clave puede pedir y confirmar enlaces")
    void exigeLaClaveDelFrontend() throws Exception {
        http.perform(post("/api/autenticacion/activaciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"activa@example.test\"}"))
                .andExpect(status().isForbidden());
        http.perform(post("/api/autenticacion/activaciones/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"abc\",\"password\":\"contraseña-larga\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un enlace vencido, desconocido o reemplazado no sirve, y la contraseña corta se rechaza")
    void enlacesQueNoSirven() throws Exception {
        importar("vencida@example.test");
        pedir("vencida@example.test").andExpect(status().isAccepted());
        String codigo = codigoEnviadoA("vencida@example.test", true);

        confirmar(codigo, "corta").andExpect(status().isBadRequest());
        confirmar("codigo-que-no-existe", "contraseña-larga").andExpect(status().isNotFound());

        jdbc.update("update activacion set vence_en = ? where codigo_hash = ?", LocalDateTime.now().minusMinutes(1),
                hash(codigo));
        confirmar(codigo, "contraseña-larga").andExpect(status().isGone());

        // Pasado el minuto de espera, un enlace nuevo reemplaza al anterior.
        jdbc.update("update activacion set creada_en = ? where codigo_hash = ?", LocalDateTime.now().minusMinutes(5),
                hash(codigo));
        clearInvocations(correo);
        pedir("vencida@example.test").andExpect(status().isAccepted());
        String nuevo = codigoEnviadoA("vencida@example.test", true);
        confirmar(codigo, "contraseña-larga").andExpect(status().isNotFound());
        confirmar(nuevo, "contraseña-larga").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Dos pedidos seguidos mandan un solo correo")
    void unSoloCorreoPorMinuto() throws Exception {
        importar("apurada@example.test");
        clearInvocations(correo);
        pedir("apurada@example.test").andExpect(status().isAccepted());
        pedir("apurada@example.test").andExpect(status().isAccepted());
        verify(correo, times(1)).enviar(eq("apurada@example.test"), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("Quien ya tiene contraseña puede elegir una nueva con el mismo camino")
    void recuperarElAcceso() throws Exception {
        http.perform(post("/api/autenticacion/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("email", "olvidadiza@example.test", "nombre", "Olvidadiza",
                                "password", "la-que-olvide-2026", "rol", "ENTIDAD_BENEFICIARIA"))))
                .andExpect(status().isCreated());
        http.perform(post("/api/autenticacion/activaciones").header("X-DonaTrack-Frontend", CLAVE_FRONTEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"olvidadiza@example.test\",\"rol\":\"ENTIDAD_BENEFICIARIA\"}"))
                .andExpect(status().isAccepted());
        String codigo = codigoEnviadoA("olvidadiza@example.test", false);

        confirmar(codigo, "la-nueva-2026").andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ENTIDAD_BENEFICIARIA"));
    }

    @Test
    @DisplayName("Las cuentas internas no se activan por email")
    void soloRolesPublicos() throws Exception {
        http.perform(post("/api/autenticacion/activaciones").header("X-DonaTrack-Frontend", CLAVE_FRONTEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.test\",\"rol\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isBadRequest());
    }

    private UUID importar(String email) throws Exception {
        UUID perfil = UUID.randomUUID();
        var persona = Map.of("donanteId", perfil, "email", email, "nombre", "Persona importada", "tipoDonante", "HUMANA");
        http.perform(post("/api/autenticacion/cuentas/importadas").header("X-DonaTrack-Servicio", CuentasImportadasTest.CLAVE_SERVICIOS)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("donantes", List.of(persona)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.creadas").value(1));
        return perfil;
    }

    private ResultActions pedir(String email) throws Exception {
        return http.perform(post("/api/autenticacion/activaciones").header("X-DonaTrack-Frontend", CLAVE_FRONTEND)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("email", email))));
    }

    private ResultActions confirmar(String codigo, String password) throws Exception {
        return http.perform(post("/api/autenticacion/activaciones/confirmar").header("X-DonaTrack-Frontend", CLAVE_FRONTEND)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("codigo", codigo, "password", password))));
    }

    private static String hash(String codigo) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(codigo.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private String codigoEnviadoA(String email, boolean primeraVez) {
        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq(email), anyString(), enlace.capture(), eq(primeraVez), eq(Duration.ofHours(24)));
        assertThat(enlace.getValue()).startsWith("http://front.example.test/activar?codigo=");
        return UriComponentsBuilder.fromUriString(enlace.getValue()).build().getQueryParams().getFirst("codigo");
    }
}
