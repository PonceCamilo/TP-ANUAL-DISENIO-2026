package ar.utn.donatrack.logistica.models.flota;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

/**
 * Documento de la colección {@code camiones}.
 * El id es el UUID que ya expone la API. La patente tiene un índice único:
 * la base rechaza un segundo camión con la misma patente.
 */
@Document(collection = "camiones")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Camion {
    @Id
    private UUID id;

    @Indexed(unique = true)
    private String patente;

    private double capacidadVolumenM3;

    private double alturaM;

    private double capacidadCargaKg;

    @Builder.Default
    private EstadoCamion estado = EstadoCamion.DISPONIBLE;
}
