package ar.utn.donatrack.autenticacion.correo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Correo del enlace de activación")
class CorreoDeActivacionTest {

    @Test
    @DisplayName("El mensaje explica por qué llega, trae el enlace y cuánto dura")
    void textoDelMensaje() {
        String primera = CorreoDeActivacion.mensaje("Ana", "http://front/activar?codigo=abc", true, Duration.ofHours(24));
        assertThat(primera).contains("Hola Ana", "cargó tus datos", "http://front/activar?codigo=abc", "24 horas", "una sola vez");

        String recupero = CorreoDeActivacion.mensaje("Ana", "http://front/activar?codigo=abc", false, Duration.ofHours(1));
        assertThat(recupero).contains("contraseña nueva", "1 hora");
    }

    @Test
    @DisplayName("Con SMTP configurado manda el correo a la persona")
    void mandaElCorreo() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        new CorreoDeActivacion(proveedor(smtp), new SyncTaskExecutor(), "smtp.example.test", "donatrack@example.test", false)
                .enviar("ana@example.test", "Ana", "http://front/activar?codigo=abc", true, Duration.ofHours(24));

        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(enviado.capture());
        assertThat(enviado.getValue().getTo()).containsExactly("ana@example.test");
        assertThat(enviado.getValue().getFrom()).isEqualTo("donatrack@example.test");
        assertThat(enviado.getValue().getSubject()).isEqualTo("DonaTrack - Activá tu cuenta");
    }

    @Test
    @DisplayName("Sin servidor SMTP no intenta mandar nada")
    void sinSmtp() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        new CorreoDeActivacion(proveedor(smtp), new SyncTaskExecutor(), "", "", true)
                .enviar("ana@example.test", "Ana", "http://front/activar?codigo=abc", true, Duration.ofHours(24));
        verify(smtp, never()).send(any(SimpleMailMessage.class));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> proveedor(JavaMailSender smtp) {
        ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(smtp);
        return proveedor;
    }
}
