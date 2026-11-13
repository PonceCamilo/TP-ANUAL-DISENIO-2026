package ar.utn.donatrack.donaciones.services.logistica;

import ar.utn.donatrack.donaciones.dtos.request.RegresoDepositoCallbackDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.EN_DEPOSITO;

/**
 * La administración devolvió una entrega no recibida al depósito. La donación
 * pasa de ENTREGA_FALLIDA a EN_DEPOSITO y vuelve a aparecer para asignarla.
 * Un aviso repetido no cambia nada.
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class ManejadorRegresoADeposito {

    private final RepositorioDonaciones donaciones;
    private final DonacionesValidator validador;

    public void procesar(RegresoDepositoCallbackDTO dto) {
        Donacion donacion = validador.validarYObtenerDonacion(dto.idDonacion());
        if (donacion.estaEnEstado(EN_DEPOSITO)) {
            log.info("[LOGISTICA] La donación {} ya está en depósito.", donacion.getId());
            return;
        }
        donacion.cambiarEstadoManualmente(EN_DEPOSITO,
                "La administración la devolvió al depósito para asignarla de nuevo.");
        donaciones.guardar(donacion);
        log.info("[LOGISTICA] Donación {} de vuelta en depósito.", donacion.getId());
    }
}
