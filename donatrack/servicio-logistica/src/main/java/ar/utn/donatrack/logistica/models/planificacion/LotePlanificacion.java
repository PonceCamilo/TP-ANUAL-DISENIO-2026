package ar.utn.donatrack.logistica.models.planificacion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Documento de la colección lotes: un envío al proveedor de ruteo.
 * Las donaciones y sus direcciones van embebidas. De los camiones solo
 * se guardan los ids, porque el camión es otro documento.
 */
@Document(collection = "lotes")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LotePlanificacion {
    @Id
    private UUID id;

    @Builder.Default
    private List<UUID> camionIds = new ArrayList<>();

    @Builder.Default
    private List<DonacionLote> donaciones = new ArrayList<>();

    private EstadoLote estado;

    private String tokenCorrelacion;

    private LocalDateTime fechaEnvio;

    private LocalDateTime fechaRespuesta;
}
