package ar.utn.donatrack.notificaciones.factory;

import ar.utn.donatrack.notificaciones.interfaces.services.NotificadorInterface;
import ar.utn.donatrack.notificaciones.model.medios.Email;
import ar.utn.donatrack.notificaciones.model.medios.Sms;
import ar.utn.donatrack.notificaciones.model.medios.WhatsApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificadorFactory - resolución de notificadores por medio")
class NotificadorFactoryTest {

    @Mock
    private NotificadorInterface notificadorEmail;

    @Mock
    private NotificadorInterface notificadorSms;

    @Mock
    private NotificadorInterface notificadorWhatsApp;

    private NotificadorFactory factory;

    @BeforeEach
    void prepararEscenario() {
        when(notificadorEmail.getMedio()).thenReturn(new Email());
        when(notificadorSms.getMedio()).thenReturn(new Sms());
        when(notificadorWhatsApp.getMedio()).thenReturn(new WhatsApp());

        factory = new NotificadorFactory(List.of(notificadorEmail, notificadorSms, notificadorWhatsApp));
    }

    @Nested
    @DisplayName("Resolución correcta de notificadores")
    class ResolucionCorrecta {

        @Test
        @DisplayName("obtenerPara('EMAIL') devuelve el notificador de email")
        void resuelveEmail() {
            assertThat(factory.obtenerPara("EMAIL")).isEqualTo(notificadorEmail);
        }

        @Test
        @DisplayName("obtenerPara('SMS') devuelve el notificador de SMS")
        void resuelveSms() {
            assertThat(factory.obtenerPara("SMS")).isEqualTo(notificadorSms);
        }

        @Test
        @DisplayName("obtenerPara('WHATSAPP') devuelve el notificador de WhatsApp")
        void resuelveWhatsApp() {
            assertThat(factory.obtenerPara("WHATSAPP")).isEqualTo(notificadorWhatsApp);
        }
    }

    @Nested
    @DisplayName("Medio desconocido")
    class MedioDesconocido {

        @Test
        @DisplayName("obtenerPara() con medio desconocido lanza IllegalArgumentException")
        void medioDesconocidoLanzaExcepcion() {
            assertThatThrownBy(() -> factory.obtenerPara("TELEGRAM"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("El mensaje de error incluye el medio desconocido")
        void mensajeDeErrorIncluyeMedio() {
            assertThatThrownBy(() -> factory.obtenerPara("TELEGRAM"))
                    .hasMessageContaining("TELEGRAM");
        }
    }
}