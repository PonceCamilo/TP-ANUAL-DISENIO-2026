package ar.utn.donatrack.logisticaexterna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Segundo proveedor de logística ("otro servicio potencial que cumple igual
 * objetivo", según la consigna del broker).
 *
 * Es independiente de servicio-logistica: otro proceso, otro puerto, sus
 * propios datos. Modela una empresa de terceros que trabaja distinto: no
 * planifica rutas ni expone su flota, recibe envíos de a uno, les asigna un
 * tracking y avisa los cambios de estado por webhook con su propio formato.
 * Por eso el broker necesita un Adapter distinto para cada proveedor.
 */
@SpringBootApplication
public class ServicioLogisticaExternaApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ServicioLogisticaExternaApp.class, args);
        String puerto = context.getEnvironment().getProperty("server.port");
        System.out.println("Escuchando en el puerto: " + puerto);
    }
}
