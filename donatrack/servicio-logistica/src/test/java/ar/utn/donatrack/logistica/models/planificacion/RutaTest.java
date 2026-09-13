package ar.utn.donatrack.logistica.models.planificacion;

import ar.utn.donatrack.logistica.models.entrega.Entrega;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de Ruta como agregado: aplanar entregas de todas las paradas y
 * localizar la parada que contiene una entrega. Esos dos métodos son los
 * que usan inicio de ruta, cierre de ruta y la query inversa entrega→ruta.
 */
@DisplayName("Ruta - entregas del agregado y búsqueda de parada")
class RutaTest {

    private Entrega entrega(UUID id) {
        return Entrega.builder().id(id).idDonacion(UUID.randomUUID()).build();
    }

    private Parada paradaCon(Entrega... entregas) {
        return Parada.builder()
                .id(UUID.randomUUID())
                .orden(1)
                .idEntidadBeneficiaria(UUID.randomUUID())
                .entregas(List.of(entregas))
                .build();
    }

    @Nested
    @DisplayName("obtenerEntregas()")
    class ObtenerEntregas {

        @Test
        @DisplayName("Concatena las entregas de todas las paradas, en orden")
        void concatenaParadas() {
            Entrega primera = entrega(UUID.randomUUID());
            Entrega segunda = entrega(UUID.randomUUID());
            Entrega tercera = entrega(UUID.randomUUID());
            Ruta ruta = Ruta.builder()
                    .id(UUID.randomUUID())
                    .paradas(List.of(paradaCon(primera, segunda), paradaCon(tercera)))
                    .build();

            assertThat(ruta.obtenerEntregas())
                    .extracting(Entrega::getId)
                    .containsExactly(primera.getId(), segunda.getId(), tercera.getId());
        }

        @Test
        @DisplayName("Sin paradas (null) devuelve lista vacía, no null")
        void sinParadas() {
            Ruta ruta = Ruta.builder().id(UUID.randomUUID()).build();

            assertThat(ruta.obtenerEntregas()).isEmpty();
        }

        @Test
        @DisplayName("Una parada sin lista de entregas se saltea")
        void paradaSinEntregas() {
            Parada vacia = Parada.builder()
                    .id(UUID.randomUUID())
                    .orden(1)
                    .build();
            Entrega unica = entrega(UUID.randomUUID());
            Ruta ruta = Ruta.builder()
                    .id(UUID.randomUUID())
                    .paradas(List.of(vacia, paradaCon(unica)))
                    .build();

            assertThat(ruta.obtenerEntregas()).containsExactly(unica);
        }
    }

    @Nested
    @DisplayName("buscarParadaPorEntregaId()")
    class BuscarParada {

        @Test
        @DisplayName("Devuelve la parada que contiene esa entrega")
        void encuentraLaParada() {
            Entrega buscada = entrega(UUID.randomUUID());
            Parada destino = paradaCon(buscada);
            Ruta ruta = Ruta.builder()
                    .id(UUID.randomUUID())
                    .paradas(List.of(paradaCon(entrega(UUID.randomUUID())), destino))
                    .build();

            assertThat(ruta.buscarParadaPorEntregaId(buscada.getId())).containsSame(destino);
        }

        @Test
        @DisplayName("Devuelve Optional vacío si ninguna parada tiene esa entrega")
        void noEncuentra() {
            Ruta ruta = Ruta.builder()
                    .id(UUID.randomUUID())
                    .paradas(List.of(paradaCon(entrega(UUID.randomUUID()))))
                    .build();

            assertThat(ruta.buscarParadaPorEntregaId(UUID.randomUUID())).isEmpty();
        }

        @Test
        @DisplayName("Sin paradas no rompe")
        void sinParadas() {
            Ruta ruta = Ruta.builder().id(UUID.randomUUID()).build();

            assertThat(ruta.buscarParadaPorEntregaId(UUID.randomUUID())).isEmpty();
        }
    }
}
