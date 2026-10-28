package ar.utn.donatrack.donaciones.services.logistica;

import ar.utn.donatrack.donaciones.dtos.request.EntregaFallidaCallbackDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aviso de logística: la entrega no pudo concretarse. La donación pasa a
 * ENTREGA_FALLIDA (con el motivo y si puede replanificarse en el historial) y se
 * avisa a la entidad, a la persona donante y a la persona administradora.
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
public class ManejadorEntregaFallida {

    private final RepositorioDonaciones donaciones;
    private final DonacionesValidator validador;
    private final ParticipantesDeDonaciones participantes;
    private final Notificador notificador;

    public void procesar(EntregaFallidaCallbackDTO dto) {
        log.info("[LOGISTICA] Entrega fallida para donación {}. Motivo: {}", dto.idDonacion(), dto.motivoFallo());

        Donacion donacion = validador.validarYObtenerDonacion(dto.idDonacion());
        String justificacion = dto.motivoFallo()
                + (dto.replanificable() ? " | Puede ser replanificada." : " | No puede ser replanificada.");
        if (!donacion.registrarEntregaFallida(justificacion)) {
            log.info("[LOGISTICA] Entrega fallida repetida para la donación {}: sin cambios.", donacion.getId());
            return;
        }
        donaciones.guardar(donacion);

        notificador.entregaFallida(donacion.getId(), participantes.donanteDe(donacion), participantes.entidadDe(donacion),
                dto.motivoFallo(), dto.replanificable());
    }
}
