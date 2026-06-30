package ar.utn.donatrack.logistica.eventos;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class EntregaEvento {
    private TipoEventoLogistica tipo;
    private UUID entregaId;
    private UUID idDonacion;
    private UUID idEntidadBeneficiaria;
    private UUID idDonante;
    private UUID camionId;
    private UUID rutaId;
    private List<String> fotosComprobante;
    private String motivo;
}
