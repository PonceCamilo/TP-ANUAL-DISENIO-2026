package ar.utn.donatrack.brokerlogistica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Broker de integración entre Donaciones y los servicios de logística.
 *
 *   Ida:    Donaciones → broker → (servicio-logistica | servicio-logistica-externa)
 *           El broker elige el proveedor (Strategy) y traduce al contrato de
 *           cada uno (Adapter).
 *   Vuelta: proveedores → broker → Donaciones
 *           Cada proveedor avisa sus eventos con su propio formato; el broker
 *           los traduce a los callbacks que ya expone Donaciones
 *           (/logistica/eventos/...). Reemplaza al relay de n8n.
 */
@SpringBootApplication
public class ServicioBrokerLogisticaApp {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ServicioBrokerLogisticaApp.class, args);
        String puerto = context.getEnvironment().getProperty("server.port");
        System.out.println("Escuchando en el puerto: " + puerto);
    }
}
