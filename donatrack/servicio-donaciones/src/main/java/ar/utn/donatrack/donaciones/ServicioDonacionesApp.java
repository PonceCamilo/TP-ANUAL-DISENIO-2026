package ar.utn.donatrack.donaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sin @EnableScheduling, Spring ignora silenciosamente las anotaciones @Scheduled:
 * ni la notificación a donantes inactivos (InactividadDonantesService) ni la
 * ejecución nocturna de los algoritmos de asignación (AsignacionBatchService)
 * llegaban a correr.
 */
@EnableScheduling
@SpringBootApplication
public class ServicioDonacionesApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ServicioDonacionesApp.class, args);
        String puerto = context.getEnvironment().getProperty("server.port");
        System.out.println("Escuchando en el puerto: " + puerto);
    }
}