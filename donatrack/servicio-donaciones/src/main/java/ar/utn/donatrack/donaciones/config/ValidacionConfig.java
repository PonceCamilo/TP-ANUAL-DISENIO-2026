package ar.utn.donatrack.donaciones.config;

import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Las validaciones de fechas de los pedidos (@FutureOrPresent, @Future, ...)
 * usan el día de Argentina, como el resto del servicio (FechaHoraArgentina).
 *
 * Sin esto usaban el reloj y la zona del servidor: en un contenedor en UTC,
 * entre las 21 y las 24 de Argentina "hoy" ya era el día siguiente, y una
 * campaña que empezaba ese mismo día se rechazaba por empezar en el pasado.
 */
@Configuration
public class ValidacionConfig {

    @Bean
    public ValidationConfigurationCustomizer relojDeArgentina() {
        return configuracion -> configuracion.clockProvider(FechaHoraArgentina::reloj);
    }
}
