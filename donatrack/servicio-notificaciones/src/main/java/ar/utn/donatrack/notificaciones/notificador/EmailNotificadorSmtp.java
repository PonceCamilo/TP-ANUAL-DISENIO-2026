package ar.utn.donatrack.notificaciones.notificador;

import ar.utn.donatrack.notificaciones.model.Notificacion;
import ar.utn.donatrack.notificaciones.model.medios.Email;
import ar.utn.donatrack.notificaciones.model.medios.MedioNotificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * PATRÓN STRATEGY — implementación concreta: EMAIL.
 * Integración real: envía el correo de verdad vía SMTP (Gmail) usando
 * JavaMailSender. Reemplaza al mock que solo logueaba por consola.
 *
 * Fíjense que para pasar de mock a real no tocamos ni el Service, ni
 * la Factory, ni el DTO: solo esta clase cambió — así es como debería
 * funcionar Strategy.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificadorSmtp extends NotificadorBase {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    public MedioNotificacion getMedio() {
        return new Email();
    }

    @Override
    protected void realizarEnvio(Notificacion notificacion) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(notificacion.getDestinatario());
        mensaje.setSubject("DonaTrack - Notificación");
        mensaje.setText(notificacion.getMensaje());
        mailSender.send(mensaje);
    }

    protected String getEtiqueta() {
        return "EMAIL";
    }
}
