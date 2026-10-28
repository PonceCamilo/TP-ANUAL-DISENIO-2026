package ar.utn.donatrack.donaciones.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Properties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los endpoints de Actuator con la configuración de producción: las claves
 * management.* se toman del application.properties del servicio (los tests usan
 * el suyo y no lo leen). Sin RabbitMQ levantado, la salud tiene que dar UP: los
 * avisos esperan en la bandeja de salida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Actuator: salud y métricas para operar el contenedor")
class ActuatorTest {

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void configuracionDeProduccion(DynamicPropertyRegistry registro) throws IOException {
        Properties produccion = PropertiesLoaderUtils.loadProperties(
                new FileSystemResource("src/main/resources/application.properties"));
        produccion.stringPropertyNames().stream()
                .filter(clave -> clave.startsWith("management."))
                .forEach(clave -> registro.add(clave, () -> produccion.getProperty(clave)));
    }

    @Test
    @DisplayName("/actuator/health da UP, también en liveness y readiness, aunque no haya RabbitMQ")
    void salud() throws Exception {
        for (String ruta : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            mvc.perform(get(ruta))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Test
    @DisplayName("Expone info y métricas, y nada más")
    void soloLoNecesario() throws Exception {
        mvc.perform(get("/actuator/metrics/jvm.memory.used")).andExpect(status().isOk());
        mvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La respuesta lleva el id de correlación del pedido")
    void idDeCorrelacion() throws Exception {
        mvc.perform(get("/actuator/health").header(IdDeCorrelacion.ENCABEZADO, "chequeo-1"))
                .andExpect(header().string(IdDeCorrelacion.ENCABEZADO, "chequeo-1"));
    }
}
