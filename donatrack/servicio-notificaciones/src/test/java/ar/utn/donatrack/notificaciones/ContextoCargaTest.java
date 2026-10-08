package ar.utn.donatrack.notificaciones;

import ar.utn.donatrack.notificaciones.interfaces.services.NotificadorInterface;
import ar.utn.donatrack.notificaciones.model.medios.MediosNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifica que el contexto de Spring cargue.
 *
 * POR QUÉ EXISTE ESTE TEST: hasta la Entrega 4 este servicio NO ARRANCABA. Los
 * @Value de los notificadores (twilio.*, spring.mail.username,
 * notificaciones.discord.webhook-url) no tenían valor por defecto y esas
 * properties no estaban definidas, así que Spring fallaba con
 * "Could not resolve placeholder" y el contenedor moría con exit 1.
 *
 * Nadie lo detectó durante semanas porque TODOS los tests eran unitarios con
 * mocks: ninguno levantaba el contexto. Un test de carga de contexto es el
 * único que detecta esta clase de fallo, y cuesta diez líneas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Carga del contexto de Spring")
class ContextoCargaTest {

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("El contexto levanta sin errores")
    void elContextoCarga() {
        assertThat(contexto).isNotNull();
    }

    @Test
    @DisplayName("Los cuatro notificadores quedan registrados")
    void losNotificadoresEstanRegistrados() {
        // Son los beans que hacían fallar el arranque. Si alguno vuelve a
        // quedarse sin su property, este test falla antes que el contenedor.
        assertThat(contexto.containsBean("emailNotificadorSmtp")).isTrue();
        assertThat(contexto.containsBean("smsNotificadorTwilio")).isTrue();
        assertThat(contexto.containsBean("whatsAppNotificadorTwilio")).isTrue();
        assertThat(contexto.containsBean("discordNotificadorWebhook")).isTrue();
    }

    @Test
    @DisplayName("El medio de TODOS los notificadores registrados se puede rehidratar")
    void todoMedioRegistradoEsRehidratable() {
        // GUARDIÁN ESTRUCTURAL. MediosNotificacion reconstruye el objeto de
        // medio a partir del nombre guardado en la base. Si un notificador
        // devuelve un medio que ese mapa no conoce, la notificación se guarda
        // bien y explota AL LEERLA, desde el @PostLoad.
        //
        // Ya pasó con DISCORD: el notificador estaba registrado y el mapa tenía
        // solo tres medios. Este test recorre los notificadores REGISTRADOS en
        // vez de una lista escrita a mano, así que cubre solo el quinto
        // notificador que alguien agregue sin tocar el mapa.
        assertThat(contexto.getBeansOfType(NotificadorInterface.class).values())
                .isNotEmpty()
                .allSatisfy(notificador -> {
                    String nombre = notificador.getMedio().getNombre();
                    assertThatCode(() -> MediosNotificacion.desde(nombre))
                            .as("El medio %s no está en el mapa de MediosNotificacion", nombre)
                            .doesNotThrowAnyException();
                });
    }

    @Test
    @DisplayName("El conversor JSON de la cola está presente")
    void elConversorJsonEstaPresente() {
        // Sin este bean el listener no puede deserializar los mensajes que
        // publican donaciones e incentivos, y la cola no funciona.
        assertThat(contexto.containsBean("jsonMessageConverter")).isTrue();
    }

    @Test
    @DisplayName("El listener de la cola está registrado")
    void elListenerEstaRegistrado() {
        assertThat(contexto.containsBean("notificacionListener")).isTrue();
    }
}
