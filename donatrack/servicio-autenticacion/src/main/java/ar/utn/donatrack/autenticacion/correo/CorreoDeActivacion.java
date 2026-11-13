package ar.utn.donatrack.autenticacion.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

/**
 * Manda el enlace de activación por email.
 *
 * Lo manda Autenticación y no el Servicio de Notificaciones porque este guarda el
 * texto de cada mensaje y lo devuelve en su API: el enlace permite elegir la
 * contraseña de la cuenta, así que no puede quedar guardado en otro servicio.
 *
 * El envío sale después de confirmar la transacción y en otro hilo: la respuesta
 * de "pedir enlace" tarda lo mismo exista o no la cuenta, y un SMTP lento o caído
 * no la demora. Sin SMTP configurado (MAIL_HOST vacío) no se envía nada; para
 * probar en local se puede mostrar el enlace en el log con
 * donatrack.auth.correo.mostrar-enlace-en-log=true.
 */
@Component
public class CorreoDeActivacion {
    private static final Logger log = LoggerFactory.getLogger(CorreoDeActivacion.class);

    private final ObjectProvider<JavaMailSender> correo;
    private final TaskExecutor hilos;
    private final String servidor;
    private final String remitente;
    private final boolean mostrarEnlaceEnLog;

    CorreoDeActivacion(ObjectProvider<JavaMailSender> correo,
                       @Qualifier("applicationTaskExecutor") TaskExecutor hilos,
                       @Value("${spring.mail.host:}") String servidor,
                       @Value("${donatrack.auth.correo.remitente:}") String remitente,
                       @Value("${donatrack.auth.correo.mostrar-enlace-en-log:false}") boolean mostrarEnlaceEnLog) {
        this.correo = correo;
        this.hilos = hilos;
        this.servidor = servidor;
        this.remitente = remitente;
        this.mostrarEnlaceEnLog = mostrarEnlaceEnLog;
    }

    /** Programa el envío para cuando se confirme la transacción en curso (o ya, si no hay una). */
    public void enviar(String destinatario, String nombre, String enlace, boolean primeraVez, Duration vigencia) {
        Runnable envio = () -> enviarAhora(destinatario, mensaje(nombre, enlace, primeraVez, vigencia), primeraVez, enlace);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    hilos.execute(envio);
                }
            });
        } else {
            hilos.execute(envio);
        }
    }

    static String mensaje(String nombre, String enlace, boolean primeraVez, Duration vigencia) {
        String motivo = primeraVez
                ? "La administración de DonaTrack cargó tus datos como donante. Para entrar por primera vez, "
                        + "elegí tu contraseña en este enlace:"
                : "Recibimos un pedido para elegir una contraseña nueva para tu cuenta de DonaTrack. "
                        + "Para hacerlo, entrá a este enlace:";
        return "Hola " + nombre + ":\n\n" + motivo + "\n\n" + enlace + "\n\n"
                + "El enlace vence en " + horas(vigencia) + " y sirve una sola vez. "
                + "Si no lo pediste, ignorá este correo: tu cuenta sigue igual.\n\nDonaTrack";
    }

    private static String horas(Duration vigencia) {
        long horas = Math.max(1, vigencia.toHours());
        return horas == 1 ? "1 hora" : horas + " horas";
    }

    private void enviarAhora(String destinatario, String texto, boolean primeraVez, String enlace) {
        JavaMailSender smtp = correo.getIfAvailable();
        if (servidor.isBlank() || smtp == null) {
            if (mostrarEnlaceEnLog) {
                log.warn("SMTP sin configurar. Enlace de activación para {}: {}", destinatario, enlace);
            } else {
                log.warn("SMTP sin configurar (MAIL_HOST): no se pudo mandar el enlace de activación a {}.", destinatario);
            }
            return;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        if (!remitente.isBlank()) {
            mensaje.setFrom(remitente);
        }
        mensaje.setTo(destinatario);
        mensaje.setSubject(primeraVez ? "DonaTrack - Activá tu cuenta" : "DonaTrack - Elegí una contraseña nueva");
        mensaje.setText(texto);
        try {
            smtp.send(mensaje);
            log.info("Enlace de activación enviado a {}.", destinatario);
        } catch (MailException e) {
            log.error("No se pudo mandar el enlace de activación a {}: {}", destinatario, e.getMessage());
        }
    }
}
