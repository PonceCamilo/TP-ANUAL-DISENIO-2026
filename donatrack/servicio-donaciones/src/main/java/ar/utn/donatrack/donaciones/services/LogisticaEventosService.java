package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.dtos.request.EntregaExitosaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntregaFallidaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.InicioRutaCallbackDTO;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donacion.CambioEstado;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.models.donacion.estado.EnTrasladoState;
import ar.utn.donatrack.donaciones.models.donacion.estado.EntregadaState;
import ar.utn.donatrack.donaciones.models.donacion.estado.EntregaFallidaState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Recibe los callbacks de eventos logísticos y dispara las notificaciones
 * correspondientes hacia el Servicio de Notificaciones.
 *
 * Regla arquitectónica (Entrega 3): el Servicio de Logística NO llama
 * directamente a notificaciones ni a incentivos; deja disponible la
 * información y es el Servicio de Donaciones quien orquesta las notificaciones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticaEventosService {

    private final DonacionesRepositoryInterface donacionesRepositorio;
    private final EntidadesBeneficiariasRepositoryInterface entidadesRepositorio;
    private final PersonaDonanteRepositoryInterface donanteRepositorio;
    private final NotificacionClient notificacionClient;
    private final IncentivosClient incentivosClient;

    // ── INICIO DE RUTA ─────────────────────────────────────────────────────────

    /**
     * El chofer inició el recorrido. Se notifica a todas las entidades
     * beneficiarias y donantes cuyas donaciones forman parte de la ruta,
     * incluyendo el link al mapa interactivo de seguimiento en tiempo real.
     */
    public void procesarInicioRuta(InicioRutaCallbackDTO dto) {
        log.info("[LOGISTICA] Inicio de ruta {}. Donaciones involucradas: {}", dto.idRuta(), dto.idsDonaciones().size());

        for (UUID idDonacion : dto.idsDonaciones()) {
            Donacion donacion = obtenerDonacionOFallar(idDonacion);

            donacion.cambiarEstado("EN_TRASLADO", "Inicio de ruta", "Chofer inició el recorrido. Ruta: " + dto.idRuta());


            notificarInicioRutaEntidad(donacion, dto.urlMapaInteractivo());
            notificarInicioRutaDonante(donacion, dto.urlMapaInteractivo());
        }
    }

    private void notificarInicioRutaEntidad(Donacion donacion, String urlMapa) {
        if (donacion.getIdEntidadBeneficiaria() == null) return;

        EntidadBeneficiaria entidad = entidadesRepositorio.obtenerPorId(donacion.getIdEntidadBeneficiaria());
        if (entidad == null) return;

        String email = obtenerEmail(entidad.getContactos());
        if (email == null) return;

        String mensaje = "El camión con tu donación está en camino. "
                + "Podés seguir la entrega en tiempo real: " + urlMapa;

        notificacionClient.enviarNotificacion(email, mensaje, "EMAIL");
    }

    private void notificarInicioRutaDonante(Donacion donacion, String urlMapa) {
        PersonaDonante donante = donanteRepositorio.obtenerPersona(donacion.getIdDonante());
        if (donante == null || donante.getEmail() == null) return;

        String mensaje = "Tu donación está siendo entregada. "
                + "Seguí el recorrido en tiempo real: " + urlMapa;

        notificacionClient.enviarNotificacion(donante.getEmail(), mensaje, "EMAIL");
    }

    // ── ENTREGA EXITOSA ────────────────────────────────────────────────────────

    /**
     * La entidad beneficiaria confirmó la recepción. Se actualiza el estado
     * de la donación a ENTREGADA y se notifica a entidad y donante con el
     * comprobante que incluye fecha, hora y patente del camión.
     */
    public void procesarEntregaExitosa(EntregaExitosaCallbackDTO dto) {
        log.info("[LOGISTICA] Entrega exitosa confirmada para donación {}", dto.idDonacion());

        Donacion donacion = obtenerDonacionOFallar(dto.idDonacion());

        donacion.cambiarEstado("ENTREGADA", "Entrega confirmada", "Entrega confirmada. Camión: " + dto.patenteCamion() + " | Fecha: " + dto.fechaHoraEntrega());

        String comprobante = armarComprobante(dto);

        notificarEntregaExitosaEntidad(donacion, comprobante);
        notificarEntregaExitosaDonante(donacion, comprobante);

        // notificar a incentivos: DonacionesExitosas depende de entregas confirmadas por logística
        PersonaDonante donante = donanteRepositorio.obtenerPersona(donacion.getIdDonante());
        if (donante != null && donante.getEmail() != null) {
            incentivosClient.notificarDonacionExitosa(
                    donacion.getIdDonante(),
                    donante.getEmail(),
                    "EMAIL"
            );
        }
    }
    private void notificarEntregaExitosaEntidad(Donacion donacion, String comprobante) {
        if (donacion.getIdEntidadBeneficiaria() == null) return;

        EntidadBeneficiaria entidad = entidadesRepositorio.obtenerPorId(donacion.getIdEntidadBeneficiaria());
        if (entidad == null) return;

        String email = obtenerEmail(entidad.getContactos());
        if (email == null) return;

        String mensaje = "¡La donación fue recibida exitosamente! " + comprobante;

        notificacionClient.enviarNotificacion(email, mensaje, "EMAIL");
    }

    private void notificarEntregaExitosaDonante(Donacion donacion, String comprobante) {
        PersonaDonante donante = donanteRepositorio.obtenerPersona(donacion.getIdDonante());
        if (donante == null || donante.getEmail() == null) return;

        String mensaje = "¡Tu donación llegó a destino! " + comprobante;

        notificacionClient.enviarNotificacion(donante.getEmail(), mensaje, "EMAIL");
    }

    private String armarComprobante(EntregaExitosaCallbackDTO dto) {
        return "Comprobante de entrega — Camión: " + dto.patenteCamion()
                + " | Fecha y hora: " + dto.fechaHoraEntrega();
    }

    // ── ENTREGA FALLIDA ────────────────────────────────────────────────────────

    /**
     * La entrega no pudo concretarse. Se actualiza el estado a ENTREGA_FALLIDA
     * y se notifica a la entidad, al donante y al administrador del sistema.
     * Si la donación es replanificable, queda registrado en el historial.
     */
    public void procesarEntregaFallida(EntregaFallidaCallbackDTO dto) {
        log.info("[LOGISTICA] Entrega fallida para donación {}. Motivo: {}", dto.idDonacion(), dto.motivoFallo());

        Donacion donacion = obtenerDonacionOFallar(dto.idDonacion());

        String justificacion = dto.motivoFallo()
                + (dto.replanificable() ? " | Puede ser replanificada." : " | No puede ser replanificada.");

        donacion.cambiarEstado("ENTREGA_FALLIDA", "Entrega fallida", justificacion);

        notificarEntregaFallidaEntidad(donacion, dto.motivoFallo(), dto.replanificable());
        notificarEntregaFallidaDonante(donacion, dto.motivoFallo(), dto.replanificable());
        notificarEntregaFallidaAdmin(donacion, dto.motivoFallo(), dto.replanificable());
    }

    private void notificarEntregaFallidaEntidad(Donacion donacion, String motivo, boolean replanificable) {
        if (donacion.getIdEntidadBeneficiaria() == null) return;

        EntidadBeneficiaria entidad = entidadesRepositorio.obtenerPorId(donacion.getIdEntidadBeneficiaria());
        if (entidad == null) return;

        String email = obtenerEmail(entidad.getContactos());
        if (email == null) return;

        String mensaje = "La entrega de tu donación no pudo realizarse. Motivo: " + motivo
                + (replanificable ? " Será replanificada próximamente." : "");

        notificacionClient.enviarNotificacion(email, mensaje, "EMAIL");
    }

    private void notificarEntregaFallidaDonante(Donacion donacion, String motivo, boolean replanificable) {
        PersonaDonante donante = donanteRepositorio.obtenerPersona(donacion.getIdDonante());
        if (donante == null || donante.getEmail() == null) return;

        String mensaje = "La entrega de tu donación no pudo concretarse. Motivo: " + motivo
                + (replanificable ? " Se intentará nuevamente." : "");

        notificacionClient.enviarNotificacion(donante.getEmail(), mensaje, "EMAIL");
    }

    private void notificarEntregaFallidaAdmin(Donacion donacion, String motivo, boolean replanificable) {
        // En este punto, el administrador se notifica al email institucional configurado.
        // En Entrega 4 se integrará con el módulo de autenticación para obtener
        // los emails reales de los administradores registrados en el sistema.
        String emailAdmin = "admin@donatrack.org";
        String mensaje = "ALERTA: Entrega fallida para donación " + donacion.getId()
                + ". Motivo: " + motivo
                + (replanificable ? " | Replanificable." : " | No replanificable.");

        notificacionClient.enviarNotificacion(emailAdmin, mensaje, "EMAIL");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Donacion obtenerDonacionOFallar(UUID id) {
        Donacion donacion = donacionesRepositorio.obtenerPorId(id);
        if (donacion == null) {
            throw new DonacionNoEncontradaException(id);
        }
        return donacion;
    }

    private String obtenerEmail(List<MedioDeContacto> contactos) {
        if (contactos == null) return null;
        return contactos.stream()
                .filter(c -> c instanceof Email)
                .map(MedioDeContacto::getValor)
                .findFirst()
                .orElse(null);
    }
}
