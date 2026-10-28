package ar.utn.donatrack.donaciones.notificaciones;

import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.models.contacto.Canal;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Punto único para avisarle algo a una persona donante, a una entidad
 * beneficiaria o a la persona administradora.
 *
 * Resuelve el destinatario y el canal: a la persona donante se le escribe por
 * su contacto predeterminado (email, SMS o WhatsApp, como pide el enunciado) y,
 * si no tiene, a su email; a la entidad, por su primer email o, si no tiene,
 * por su primer contacto. Antes cada service elegía por su cuenta y casi todos
 * mandaban siempre "EMAIL" al email de la persona, aunque hubiera elegido otro
 * canal.
 *
 * Los textos de los avisos también viven acá, en un solo lugar.
 */
@Component
@RequiredArgsConstructor
public class Notificador {

    private final NotificacionClient cliente;
    private final DonacionesProperties propiedades;

    /** A quién y por qué canal se le escribe. */
    public record Destino(String valor, Canal canal) {
    }

    // ── Destinatarios ──────────────────────────────────────────────────────

    public Optional<Destino> destinoDe(PersonaDonante donante) {
        if (donante == null) {
            return Optional.empty();
        }
        MedioDeContacto predeterminado = donante.getMedioContactoPredeterminado();
        if (predeterminado != null && predeterminado.getValor() != null) {
            return Optional.of(new Destino(predeterminado.getValor(), predeterminado.canal()));
        }
        return Optional.ofNullable(donante.getEmail()).map(email -> new Destino(email, Canal.EMAIL));
    }

    public Optional<Destino> destinoDe(EntidadBeneficiaria entidad) {
        return Optional.ofNullable(entidad)
                .flatMap(EntidadBeneficiaria::contactoParaNotificar)
                .map(contacto -> new Destino(contacto.getValor(), contacto.canal()));
    }

    // ── Avisos ─────────────────────────────────────────────────────────────

    /** Si la entidad no existe (o no se pudo leer), igual avisa a la persona, sin el nombre. */
    public void donacionAsignada(PersonaDonante donante, EntidadBeneficiaria entidad) {
        enviar(destinoDe(entidad), "Se te asignó una nueva donación según tus necesidades registradas.");
        enviar(destinoDe(donante), entidad == null
                ? "Tu donación fue asignada a una entidad beneficiaria."
                : "Tu donación fue asignada a " + entidad.getRazonSocial() + ".");
    }

    public void inicioDeRuta(PersonaDonante donante, EntidadBeneficiaria entidad, String urlMapa) {
        enviar(destinoDe(entidad), "El camión con tu donación está en camino. "
                + "Podés seguir la entrega en tiempo real: " + urlMapa);
        enviar(destinoDe(donante), "Tu donación está siendo entregada. "
                + "Seguí el recorrido en tiempo real: " + urlMapa);
    }

    public void entregaExitosa(PersonaDonante donante, EntidadBeneficiaria entidad, String comprobante) {
        enviar(destinoDe(entidad), "¡La donación fue recibida exitosamente! " + comprobante);
        enviar(destinoDe(donante), "¡Tu donación llegó a destino! " + comprobante);
    }

    public void entregaFallida(UUID idDonacion, PersonaDonante donante, EntidadBeneficiaria entidad,
                               String motivo, boolean replanificable) {
        enviar(destinoDe(entidad), "La entrega de tu donación no pudo realizarse. Motivo: " + motivo
                + (replanificable ? " Será replanificada próximamente." : ""));
        enviar(destinoDe(donante), "La entrega de tu donación no pudo concretarse. Motivo: " + motivo
                + (replanificable ? " Se intentará nuevamente." : ""));
        enviar(Optional.of(new Destino(propiedades.notificaciones().emailAdmin(), Canal.EMAIL)),
                "ALERTA: Entrega fallida para donación " + idDonacion + ". Motivo: " + motivo
                        + (replanificable ? " | Replanificable." : " | No replanificable."));
    }

    /** Devuelve false si la persona no tiene ningún canal por el que avisarle. */
    public boolean inactividad(PersonaDonante donante) {
        Optional<Destino> destino = destinoDe(donante);
        enviar(destino, "¡Hace más de " + propiedades.inactividad().dias() + " días que no tenemos noticias tuyas! "
                + "Tu ayuda hace la diferencia: ingresá a DonaTrack y sumá una nueva donación.");
        return destino.isPresent();
    }

    private void enviar(Optional<Destino> destino, String mensaje) {
        destino.ifPresent(d -> cliente.enviarNotificacion(d.valor(), mensaje, d.canal().name()));
    }
}
