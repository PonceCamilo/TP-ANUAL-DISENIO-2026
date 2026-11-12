package ar.utn.donatrack.donaciones.dtos.response;

import ar.utn.donatrack.donaciones.models.donacion.ProveedorLogistica;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DonacionResponseDTO {
    private final UUID id;
    private final UUID idDonante;
    private final UUID idEntidadBeneficiaria;
    private final String descripcion;
    private final LocalDateTime fechaDonacion;
    private final String subcategoria;
    private final String estado;

    /** Proveedor de logística que tomó la entrega y su id de seguimiento; null si todavía no se despachó. */
    private final ProveedorLogistica proveedorLogistica;
    private final String idSeguimientoLogistica;

    /** Cuándo se entregó; null si todavía no llegó a ENTREGADA (Entrega 6). */
    private final LocalDateTime fechaEntrega;
    /** URLs de las fotos que cargó la entidad al recibirla; vacía si no hay (Entrega 6). */
    private final List<String> fotosEntrega;

    private final List<BienResponseDTO> bienes;
    private final List<CambioEstadoResponseDTO> historialEstados;
}
