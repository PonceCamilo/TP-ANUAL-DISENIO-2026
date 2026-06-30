package ar.utn.donatrack.logistica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ServicioLogisticaApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ServicioLogisticaApp.class, args);
        String puerto = context.getEnvironment().getProperty("server.port");
        System.out.println("Escuchando en el puerto: " + puerto);
    }
}
