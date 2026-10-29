package ar.utn.donatrack.incentivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *@EnableScheduling activa la ejecucion de tareas programadas.
 */
@EnableScheduling
@SpringBootApplication
public class ServicioIncentivosApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ServicioIncentivosApp.class, args);
        String puerto = context.getEnvironment().getProperty("server.port");
        System.out.println("Escuchando en el puerto: " + puerto);
    }
}
