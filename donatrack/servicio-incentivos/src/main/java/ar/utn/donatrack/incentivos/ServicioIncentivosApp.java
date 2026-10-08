package ar.utn.donatrack.incentivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sin @EnableScheduling, Spring ignora silenciosamente las anotaciones
 * @Scheduled: el código compila, el servicio arranca sin un solo warning y los
 * procesos nunca corren.
 *
 * Los dos de IncentivosJob estaban en esa situación:
 *   - revisarRachasCadaTreintaDias(), que implementa la pérdida de misiones
 *     ("racha" se pierde si no se dona durante un mes completo). Es el ítem 6
 *     del checklist que marcó la cátedra para revisar en la Entrega 3.
 *   - calcularRankingMensual(), que cierra el ranking el último día del mes.
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
