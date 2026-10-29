package ar.utn.donatrack.donaciones.services.logistica;

import ar.utn.donatrack.donaciones.dtos.request.EntregaExitosaCallbackDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aviso de logística: la entidad confirmó la recepción. La donación pasa a
 * ENTREGADA con la fecha que informa logística y se avisa a entidad y donante
 * con el comprobante (fecha, hora y patente del camión). Imputar lo entregado a
 * las necesidades y avisar a incentivos lo hacen los oyentes del evento de
 * entrega.
 *
 * Regla arquitectónica (Entrega 3): el Servicio de Logística NO llama
 * directamente a notificaciones ni a incentivos; deja disponible la
 * información y es el Servicio de Donaciones quien orquesta los avisos.
 *
 * Cada aviso es una transacción y es idempotente: un aviso repetido responde
 * bien sin cambiar nada ni volver a notificar.
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class ManejadorEntregaExitosa {

    private final RepositorioDonaciones donaciones;
    private final DonacionesValidator validador;
    private final ParticipantesDeDonaciones participantes;
    private final Notificador notificador;

    public void procesar(EntregaExitosaCallbackDTO dto) {
        log.info("[LOGISTICA] Entrega exitosa confirmada para donación {}", dto.idDonacion());

        Donacion donacion = validador.validarYObtenerDonacion(dto.idDonacion());
        if (!donacion.registrarEntrega(dto.fechaHoraEntrega(),
                "Entrega confirmada. Camión: " + dto.patenteCamion() + " | Fecha: " + dto.fechaHoraEntrega())) {
            log.info("[LOGISTICA] Entrega exitosa repetida para la donación {}: sin cambios.", donacion.getId());
            return;
        }
        donaciones.guardar(donacion);

        notificador.entregaExitosa(participantes.donanteDe(donacion), participantes.entidadDe(donacion),
                "Comprobante de entrega — Camión: " + dto.patenteCamion() + " | Fecha y hora: " + dto.fechaHoraEntrega());
    }
}
