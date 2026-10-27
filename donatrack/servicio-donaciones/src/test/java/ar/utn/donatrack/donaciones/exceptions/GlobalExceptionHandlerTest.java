package ar.utn.donatrack.donaciones.exceptions;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.comunes.EstadoDesconocidoException;
import ar.utn.donatrack.donaciones.exceptions.comunes.PaginaInvalidaException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.logisticaExceptions.LogisticaNoDisponibleException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler: cada categoría define el código HTTP")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class ControladorQueFalla {
        @GetMapping("/falla/{tipo}")
        String fallar(@PathVariable String tipo) {
            throw switch (tipo) {
                case "no-encontrado" -> new DonacionNoEncontradaException(UUID.randomUUID());
                case "invalida" -> new PaginaInvalidaException("página fuera de rango");
                case "conflicto" -> new EmailYaRegistradoException("ana@mail.com");
                case "regla" -> new TransicionDeEstadoIlegalException("EN_DEPOSITO", "ENTREGADA");
                case "no-disponible" -> new LogisticaNoDisponibleException("sin proveedores");
                case "archivo" -> new MaxUploadSizeExceededException(1024);
                case "estado-corrupto" -> new EstadoDesconocidoException("donación", "INVENTADO");
                default -> new IllegalStateException("cualquier otra cosa");
            };
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorQueFalla())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void noEncontradoResponde404() throws Exception {
        mockMvc.perform(get("/falla/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void solicitudInvalidaResponde400() throws Exception {
        mockMvc.perform(get("/falla/invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("página fuera de rango"));
    }

    @Test
    void conflictoResponde409() throws Exception {
        mockMvc.perform(get("/falla/conflicto")).andExpect(status().isConflict());
    }

    @Test
    void reglaDeNegocioResponde422() throws Exception {
        mockMvc.perform(get("/falla/regla"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"));
    }

    @Test
    void servicioNoDisponibleResponde503() throws Exception {
        mockMvc.perform(get("/falla/no-disponible")).andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("un archivo que supera el límite de subida es un 413, no un 500")
    void archivoDemasiadoGrandeResponde413() throws Exception {
        mockMvc.perform(get("/falla/archivo"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    @DisplayName("un estado desconocido en la base es un error del servidor, no del cliente")
    void estadoPersistidoDesconocidoResponde500() throws Exception {
        mockMvc.perform(get("/falla/estado-corrupto")).andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("IllegalStateException ya no se informa como 409: es un error del servidor")
    void illegalStateResponde500() throws Exception {
        mockMvc.perform(get("/falla/otra"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error inesperado. Contacte al administrador."));
    }

    @Test
    @DisplayName("las claves del JSON de error salen siempre en el mismo orden")
    void ordenDeClavesEstable() throws Exception {
        String cuerpo = mockMvc.perform(get("/falla/conflicto")).andReturn().getResponse().getContentAsString();
        assertThat(cuerpo.indexOf("\"timestamp\""))
                .isLessThan(cuerpo.indexOf("\"status\""));
        assertThat(cuerpo.indexOf("\"status\"")).isLessThan(cuerpo.indexOf("\"error\""));
        assertThat(cuerpo.indexOf("\"error\"")).isLessThan(cuerpo.indexOf("\"message\""));
    }
}
