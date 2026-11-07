package ar.utn.donatrack.donaciones.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DonacionesProperties: parámetros del servicio leídos de la configuración")
class DonacionesPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(DonacionesProperties.class)
    static class Config {
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(Config.class);

    @Test
    @DisplayName("sin configurar nada, los valores son los históricos")
    void defaults() {
        contexto.run(ctx -> {
            DonacionesProperties propiedades = ctx.getBean(DonacionesProperties.class);
            assertThat(propiedades).isEqualTo(PropiedadesDePrueba.porDefecto());
        });
    }

    @Test
    void tomaLosValoresConfigurados() {
        contexto.withPropertyValues(
                        "donaciones.inactividad.dias=30",
                        "donaciones.importacion.tamanio-lote=1000",
                        "donaciones.notificaciones.email-admin=operaciones@donatrack.org")
                .run(ctx -> {
                    DonacionesProperties propiedades = ctx.getBean(DonacionesProperties.class);
                    assertThat(propiedades.inactividad().dias()).isEqualTo(30);
                    assertThat(propiedades.importacion().tamanioLote()).isEqualTo(1000);
                    assertThat(propiedades.notificaciones().emailAdmin()).isEqualTo("operaciones@donatrack.org");
                    assertThat(propiedades.procesos().cronMatchmaking()).isEqualTo("0 30 3 * * *");
                });
    }

    @Test
    @DisplayName("un valor inválido impide arrancar en lugar de fallar más tarde")
    void valorInvalidoFallaAlArrancar() {
        contexto.withPropertyValues("donaciones.inactividad.dias=0")
                .run(ctx -> assertThat(ctx).hasFailed());
        contexto.withPropertyValues("donaciones.notificaciones.email-admin=no-es-un-email")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
