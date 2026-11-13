package ar.utn.donatrack.logistica.seguridad;

import ar.utn.donatrack.logistica.config.SeguridadConfig;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Para los tests de la capa web que existían antes de la Entrega 6: carga la
 * seguridad real y hace que cada pedido vaya, por defecto, con un token de
 * administración. Así siguen probando lo que probaban (rutas, cuerpos, errores).
 * Un pedido con su propio .with(...) reemplaza al de administración. Los permisos
 * de cada rol se prueban en SeguridadHttpTest.
 */
@TestConfiguration
@Import({SeguridadConfig.class, ControlDeAcceso.class})
public class SeguridadDePrueba {

    @Bean
    MockMvcBuilderCustomizer pedidosComoAdministrador() {
        return mockMvc -> mockMvc.defaultRequest(get("/").with(TokensDePrueba.administrador()));
    }
}
