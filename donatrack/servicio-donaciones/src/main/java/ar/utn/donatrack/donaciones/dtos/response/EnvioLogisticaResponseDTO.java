package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.clientes.BrokerLogisticaClient;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resultado de despachar un lote de donaciones a logística.
 *
 * Reexpone lo que devolvió el broker, incluyendo proveedoresDescartados: es la
 * forma de ver el fallback en acción (queda vacío si atendió el primero).
 */
@Getter
@Builder
public class EnvioLogisticaResponseDTO {

    /** Proveedor que finalmente tomó el lote: "DONATRACK" o "EXTERNA". */
    private String proveedor;

    /** Proveedores que se probaron antes y no estaban disponibles. */
    private List<String> proveedoresDescartados;

    private List<EnvioDonacionDTO> envios;

    public static EnvioLogisticaResponseDTO desde(BrokerLogisticaClient.ResultadoEnvio resultado) {
        return EnvioLogisticaResponseDTO.builder()
                .proveedor(resultado.proveedor())
                .proveedoresDescartados(resultado.proveedoresDescartados())
                .envios(resultado.envios().stream()
                        .map(envio -> EnvioDonacionDTO.builder()
                                .idDonacion(envio.idDonacion())
                                .proveedor(envio.proveedor())
                                .idSeguimiento(envio.idSeguimiento())
                                .fechaAsignacion(envio.fechaAsignacion())
                                .build())
                        .toList())
                .build();
    }

    @Getter
    @Builder
    public static class EnvioDonacionDTO {
        private UUID idDonacion;
        private String proveedor;
        /** Id con el que el proveedor sigue la donación (lote en DONATRACK, trackingId en EXTERNA). */
        private String idSeguimiento;
        private LocalDateTime fechaAsignacion;
    }
}
