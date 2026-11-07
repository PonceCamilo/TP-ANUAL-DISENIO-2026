package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.integracion.notificaciones.NotificacionTransportAmqp;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("RelayProgramado - una corrida del relay no dura más que su bloqueo")
class RelayProgramadoTest {

    @Test
    @DisplayName("Cada corrida envía con el corte a mitad del bloqueo")
    void enviaConElCorte() {
        RelayDeMensajes relay = mock(RelayDeMensajes.class);

        new RelayProgramado(relay).enviarPendientes();

        verify(relay).enviarPendientes(RelayProgramado.CORTE);
    }

    @Test
    @DisplayName("El corte más el envío más lento posible (con los timeouts de producción) terminan antes de que venza el bloqueo")
    void elUltimoEnvioEntraEnElBloqueo() throws NoSuchMethodException, IOException {
        Duration bloqueo = Duration.parse(RelayProgramado.class.getMethod("enviarPendientes")
                .getAnnotation(SchedulerLock.class).lockAtMostFor());
        Properties produccion = PropertiesLoaderUtils.loadProperties(
                new FileSystemResource("src/main/resources/application.properties"));
        Duration envioRest = duracion(produccion, "spring.http.client.connect-timeout")
                .plus(duracion(produccion, "spring.http.client.read-timeout"));
        Duration envioAmqp = duracion(produccion, "spring.rabbitmq.connection-timeout")
                .plusMillis(NotificacionTransportAmqp.TIEMPO_CONFIRMACION_MS);
        Duration envioMasLento = envioRest.compareTo(envioAmqp) > 0 ? envioRest : envioAmqp;

        assertThat(RelayProgramado.CORTE.plus(envioMasLento)).isLessThan(bloqueo);
    }

    private static Duration duracion(Properties propiedades, String clave) {
        assertThat(propiedades).containsKey(clave);
        return DurationStyle.detectAndParse(propiedades.getProperty(clave));
    }
}
