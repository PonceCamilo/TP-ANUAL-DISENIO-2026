package ar.utn.donatrack.donaciones.notificaciones;

import ar.utn.donatrack.donaciones.models.donacion.ProveedorLogistica;

import ar.utn.donatrack.donaciones.models.donacion.Donacion;

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
        donacionAsignada(donante, entidad, null);
    }

    /**
     * Entrega 6: el aviso dice qué se asignó. Antes decía siempre "según tus
     * necesidades registradas", aunque la administración hubiera elegido la
     * entidad por otro criterio.
     */
    public void donacionAsignada(PersonaDonante donante, EntidadBeneficiaria entidad, Donacion donacion) {
        String que = donacion == null ? "" : " (" + donacion.resumen() + ")";
        enviar(destinoDe(entidad), "La organización te asignó una donación" + que
                + ". Te avisamos cuando salga hacia tu entidad.");
        enviar(destinoDe(donante), entidad == null
                ? "Tu donación" + que + " fue asignada a una entidad beneficiaria."
                : "Tu donación" + que + " fue asignada a " + entidad.getRazonSocial() + ".");
    }

    public void inicioDeRuta(PersonaDonante donante, EntidadBeneficiaria entidad, String urlMapa) {
        enviar(destinoDe(entidad), "El camión con tu donación está en camino. "
                + "Podés seguir la entrega en tiempo real: " + urlMapa);
        enviar(destinoDe(donante), "Tu donación está siendo entregada. "
                + "Seguí el recorrido en tiempo real: " + urlMapa);
    }

    /**
     * Entrega 6: con logística propia el enlace va al mapa del front (el que
     * mandaba logística apuntaba a un dominio que no existe). La persona donante
     * lo abre con su donación, que el front verifica antes de mostrar la ruta. Con
     * el proveedor externo queda el seguimiento que da ese proveedor.
     */
    public void inicioDeRuta(PersonaDonante donante, EntidadBeneficiaria entidad, Donacion donacion,
                             UUID idRuta, String urlProveedor) {
        boolean propia = donacion.getProveedorLogistica() == null
                || donacion.getProveedorLogistica() == ProveedorLogistica.DONATRACK;
        String base = propiedades.notificaciones().urlFrontend().replaceAll("/+$", "");
        String paraLaEntidad = propia ? base + "/tracking/ruta/" + idRuta : urlProveedor;
        String paraLaPersona = propia ? paraLaEntidad + "?donacion=" + donacion.getId() : urlProveedor;
        String que = donacion.resumen();
        enviar(destinoDe(entidad), "El camión con tu donación (" + que + ") está en camino. "
                + "Podés seguir la entrega en tiempo real: " + paraLaEntidad);
        enviar(destinoDe(donante), "Tu donación (" + que + ") está siendo entregada. "
                + "Seguí el recorrido en tiempo real: " + paraLaPersona);
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
                "ALERTA: Entrega fallida para donación " + idDonacion
                        + (entidad == null ? "" : " (destino: " + entidad.getRazonSocial() + ")") + ". Motivo: " + motivo
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
