package ar.utn.donatrack.donaciones.seguridad;

import ar.utn.donatrack.donaciones.config.SeguridadConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Para los tests de la capa web que existían antes de la Entrega 6: carga la
 * seguridad real (SeguridadConfig y ControlDeAcceso) y hace que cada pedido de
 * MockMvc vaya, por defecto, con un token de administración, que puede todo.
 * Así esos tests siguen probando lo que probaban (rutas, cuerpos, errores) sin
 * repetir el token en cada pedido.
 *
 * Un pedido que agrega su propio .with(...) reemplaza al de administración: así
 * se prueban los demás roles y el pedido sin token (anonymous()). Los permisos
 * de cada rol se prueban en SeguridadHttpTest, que no usa esta configuración.
 */
@TestConfiguration
@Import({SeguridadConfig.class, ControlDeAcceso.class})
public class SeguridadDePrueba {

    @Bean
    MockMvcBuilderCustomizer pedidosComoAdministrador() {
        return mockMvc -> mockMvc.defaultRequest(get("/").with(TokensDePrueba.administrador()));
    }
}
