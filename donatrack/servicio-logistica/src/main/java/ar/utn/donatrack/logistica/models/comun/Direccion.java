package ar.utn.donatrack.logistica.models.comun;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Objeto de valor propio de logística (bounded context independiente):
 * no reutiliza la Direccion de servicio-donaciones para no acoplarse
 * a un modelo ajeno. Inmutable: si la dirección cambia, se crea otra instancia.
 *
 * Se persiste en su propia tabla; el id lo genera JPA al guardarla junto con
 * la Parada o la DonacionLote que la usa.
 */
@Entity
@Table(name = "direccion")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Direccion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_direccion")
    private UUID id;

    @Column(nullable = false, length = 150)
    private String calle;

    private int numero;

    @Column(length = 100)
    private String localidad;

    @Column(length = 100)
    private String provincia;

    @Column(length = 20)
    private String codigoPostal;
}
