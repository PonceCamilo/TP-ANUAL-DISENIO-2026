package ar.utn.donatrack.logistica.models.entrega;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro inmutable del historial de una entrega (no tiene setters).
 * Se guarda en cascada con su Entrega.
 */
@Entity
@Table(name = "cambio_estado_entrega")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CambioEstadoEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_cambio")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEntrega estado;

    @Column(columnDefinition = "text")
    private String observacion;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();
}
