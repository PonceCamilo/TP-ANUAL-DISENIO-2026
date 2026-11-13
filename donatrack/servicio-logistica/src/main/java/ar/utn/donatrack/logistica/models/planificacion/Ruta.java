package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Documento de la colección rutas. Paradas, direcciones, entregas,
 * historial y fotos van embebidos. El lote y el camión se referencian por id.
 */
@Document(collection = "rutas")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Ruta {
    @Id
    private UUID id;

    private UUID loteId;

    private UUID camionId;

    /** Lo completa RutaRepository al leer. No se embebe el camión. */
    @Transient
    private Camion camion;

    private List<Parada> paradas;

    private EstadoRuta estado;

    private LocalDateTime fechaInicio;

    public List<Entrega> obtenerEntregas() {
        if (paradas == null) {
            return List.of();
        }
        return paradas.stream()
                .filter(p -> p.getEntregas() != null)
                .flatMap(p -> p.getEntregas().stream())
                .toList();
    }

    public boolean contieneDonacion(UUID idDonacion) {
        return obtenerEntregas().stream().anyMatch(entrega -> idDonacion.equals(entrega.getIdDonacion()));
    }

    public Parada paradaDeEntidad(UUID idEntidad) {
        if (paradas == null || idEntidad == null) {
            return null;
        }
        return paradas.stream()
                .filter(parada -> idEntidad.equals(parada.getIdEntidadBeneficiaria()))
                .findFirst()
                .orElse(null);
    }

    /** Una parada nueva, al final. Varias donaciones de la misma entidad comparten la que ya existe. */
    public Parada agregarParada(UUID idEntidad, String nombreEntidad, Direccion direccion) {
        if (paradas == null || !(paradas instanceof ArrayList)) {
            paradas = paradas == null ? new ArrayList<>() : new ArrayList<>(paradas);
        }
        Parada parada = Parada.builder()
                .id(UUID.randomUUID())
                .orden(paradas.size() + 1)
                .idEntidadBeneficiaria(idEntidad)
                .nombreEntidad(nombreEntidad)
                .direccion(direccion)
                .entregas(new ArrayList<>())
                .build();
        paradas.add(parada);
        return parada;
    }

    public Optional<Entrega> buscarEntrega(UUID entregaId) {
        return obtenerEntregas().stream()
                .filter(entrega -> entregaId.equals(entrega.getId()))
                .findFirst();
    }

    public Optional<Parada> buscarParadaPorEntregaId(UUID entregaId) {
        if (paradas == null) {
            return Optional.empty();
        }
        return paradas.stream()
                .filter(p -> p.getEntregas() != null
                        && p.getEntregas().stream().anyMatch(e -> entregaId.equals(e.getId())))
                .findFirst();
    }

    /** Rearma el puntero entrega → parada, que el documento no guarda. */
    public void enlazarEntregas() {
        if (paradas == null) {
            return;
        }
        for (Parada parada : paradas) {
            if (parada.getEntregas() == null) {
                continue;
            }
            for (Entrega entrega : parada.getEntregas()) {
                entrega.setParada(parada);
            }
        }
    }
}
