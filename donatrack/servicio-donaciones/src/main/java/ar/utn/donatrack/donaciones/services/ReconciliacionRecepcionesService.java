package ar.utn.donatrack.donaciones.services;

import org.springframework.transaction.annotation.Transactional;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioRecepcionesEntidad;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Recalcula cada noche el contador desnormalizado de donaciones recibidas por
 * entidad (entidad_recepcion_diaria) a partir de la tabla donacion.
 *
 * POR QUÉ: el contador se mantiene en la misma transacción que cada asignación,
 * así que en condiciones normales ya está bien. La reconciliación cubre lo que
 * esa regla no ve: un cambio hecho a mano en la base, una restauración de un
 * backup o dos asignaciones simultáneas que intentaron crear la misma fila. Con
 * esto el peor caso es un contador desfasado hasta la noche siguiente.
 *
 * Corre a las 03:00, antes del matchmaking de las 03:30, para que el algoritmo
 * de sub-atendidos trabaje con el contador recién verificado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliacionRecepcionesService {

    private final RepositorioRecepcionesEntidad recepciones;

    @Scheduled(cron = "${donaciones.procesos.cron-reconciliacion:0 0 3 * * *}", zone = FechaHoraArgentina.ZONA_ID)
    @Transactional
    public void reconciliar() {
        int filas = recepciones.reconciliar();
        log.info("[RECEPCIONES] Contador de recepciones recalculado: {} filas (entidad, día)", filas);
    }
}
