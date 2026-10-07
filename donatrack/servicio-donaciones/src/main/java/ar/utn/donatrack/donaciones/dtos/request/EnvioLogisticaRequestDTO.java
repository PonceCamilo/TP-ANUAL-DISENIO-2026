package ar.utn.donatrack.donaciones.dtos.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Pedido para despachar un lote de donaciones a logística.
 *
 * Se mandan varias juntas porque logística planifica rutas por lotes: enviarlas
 * de a una haría que cada donación termine en su propia ruta.
 */
@Getter
@Setter
@NoArgsConstructor
public class EnvioLogisticaRequestDTO {

    @NotEmpty(message = "Hay que indicar al menos una donación para despachar")
    private List<@NotNull UUID> idsDonaciones;

    /**
     * Opcional. null deja que el broker elija por prioridad y haga fallback;
     * "DONATRACK" o "EXTERNA" fuerzan ese proveedor sin fallback.
     */
    @Pattern(regexp = "DONATRACK|EXTERNA", message = "El proveedor debe ser DONATRACK o EXTERNA")
    private String proveedor;
}
