package ar.utn.donatrack.logistica.eventos;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Hecho de logística publicado hacia n8n. Es un evento único con campos por
 * tipo: no todos aplican a todos los tipos (INICIO_RUTA agrupa varias donaciones
 * en idsDonaciones; ENTREGA_CONFIRMADA/NO_RECIBIDA refieren a una sola donación).
 * El listener arma el payload según el tipo.
 */
@Getter
@Builder
public class EntregaEvento {
    private TipoEventoLogistica tipo;
    private UUID entregaId;
    private UUID rutaId;

    // INICIO_RUTA (un evento por ruta agrupando todas sus donaciones)
    private List<UUID> idsDonaciones;
    private String urlMapaInteractivo;

    // ENTREGA_CONFIRMADA / ENTREGA_NO_RECIBIDA (una donación)
    private UUID idDonacion;
    private UUID idEntidadBeneficiaria;
    private UUID idDonante;
    private UUID idCamion;
    private String patenteCamion;
    private LocalDateTime fechaHoraEntrega;
    private List<String> fotosComprobante;

    // ENTREGA_NO_RECIBIDA
    private String motivoFallo;
    private Boolean replanificable;
}
