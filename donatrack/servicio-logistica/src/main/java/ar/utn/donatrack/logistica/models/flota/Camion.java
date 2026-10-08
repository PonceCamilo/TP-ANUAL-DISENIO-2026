package ar.utn.donatrack.logistica.models.flota;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "camion")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Camion {
    @Id
    @Column(name = "id_camion")
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String patente;

    // En la base son decimal(p, 2); en el dominio se siguen usando como double.
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "capacidad_volumen_m3", precision = 10, scale = 2)
    private double capacidadVolumenM3;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "altura_m", precision = 5, scale = 2)
    private double alturaM;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "capacidad_carga_kg", precision = 10, scale = 2)
    private double capacidadCargaKg;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCamion estado = EstadoCamion.DISPONIBLE;
}
