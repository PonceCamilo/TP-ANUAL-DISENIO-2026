package ar.utn.donatrack.autenticacion.config;

import ar.utn.donatrack.autenticacion.services.AutenticacionService;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class CuentaInicial {
    @Bean
    ApplicationRunner crearAdministradorInicial(AutenticacionService cuentas, Environment entorno) {
        return args -> {
            String email = entorno.getProperty("AUTH_ADMINISTRADOR_EMAIL", "");
            String password = entorno.getProperty("AUTH_ADMINISTRADOR_PASSWORD", "");
            if (email.isBlank() != password.isBlank()) {
                throw new IllegalArgumentException("Configurá el email y la contraseña del administrador juntos.");
            }
            cuentas.crearAdministradorInicial(email, password);
        };
    }
}
