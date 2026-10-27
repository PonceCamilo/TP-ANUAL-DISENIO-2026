package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.dtos.request.EntregaExitosaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntregaFallidaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.InicioRutaCallbackDTO;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final Notificador notificador;
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
            donacionesRepositorio.guardar(donacion);

            notificador.inicioDeRuta(donanteDe(donacion), entidadDe(donacion), dto.urlMapaInteractivo());
        }
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
        donacionesRepositorio.guardar(donacion);

        PersonaDonante donante = donanteDe(donacion);
        notificador.entregaExitosa(donante, entidadDe(donacion), armarComprobante(dto));

        // notificar a incentivos: DonacionesExitosas depende de entregas confirmadas por logística
        notificador.destinoDe(donante).ifPresent(destino ->
                incentivosClient.notificarDonacionExitosa(donacion, destino.valor(), destino.canal().name()));
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
        donacionesRepositorio.guardar(donacion);

        notificador.entregaFallida(donacion.getId(), donanteDe(donacion), entidadDe(donacion),
                dto.motivoFallo(), dto.replanificable());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Donacion obtenerDonacionOFallar(UUID id) {
        Donacion donacion = donacionesRepositorio.obtenerPorId(id);
        if (donacion == null) {
            throw new DonacionNoEncontradaException(id);
        }
        return donacion;
    }

    private PersonaDonante donanteDe(Donacion donacion) {
        return donanteRepositorio.obtenerPersona(donacion.getIdDonante());
    }

    private EntidadBeneficiaria entidadDe(Donacion donacion) {
        UUID idEntidad = donacion.getIdEntidadBeneficiaria();
        return idEntidad == null ? null : entidadesRepositorio.obtenerPorId(idEntidad);
    }
}
