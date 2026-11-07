package ar.utn.donatrack.logistica.models.entrega;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del comportamiento propio de la Entrega (modelo rico).
 *
 * registrarCambio() actualiza el estado actual, la observación y acumula
 * el historial. Es el método que usan los services para no olvidarse de
 * dejar trazabilidad en cada transición.
 */
@DisplayName("Entrega - comportamiento del modelo rico")
class EntregaTest {

    private Entrega entrega;

    @BeforeEach
    void crearEntrega() {
        entrega = Entrega.builder()
                .id(UUID.randomUUID())
                .idDonacion(UUID.randomUUID())
                .build();
    }

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicial {

        @Test
        @DisplayName("Una entrega recién creada nace LISTO_PARA_ENTREGAR")
        void naceListaParaEntregar() {
            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.LISTO_PARA_ENTREGAR);
        }
    }

    @Nested
    @DisplayName("registrarCambio() - estado, observación e historial")
    class RegistrarCambio {

        @Test
        @DisplayName("Actualiza el estado actual y la observación")
        void actualizaEstadoYObservacion() {
            entrega.registrarCambio(EstadoEntrega.EN_TRASLADO, "Inicio de ruta");

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.EN_TRASLADO);
            assertThat(entrega.getObservacion()).isEqualTo("Inicio de ruta");
        }

        @Test
        @DisplayName("Cada cambio deja una entrada en el historial con estado y observación")
        void acumulaHistorial() {
            entrega.registrarCambio(EstadoEntrega.EN_TRASLADO, "Inicio de ruta");
            entrega.registrarCambio(EstadoEntrega.ENTREGADA, null);

            assertThat(entrega.getHistorial()).hasSize(2);
            assertThat(entrega.getHistorial())
                    .extracting(CambioEstadoEntrega::getEstado)
                    .containsExactly(EstadoEntrega.EN_TRASLADO, EstadoEntrega.ENTREGADA);
            assertThat(entrega.getHistorial().getFirst().getObservacion()).isEqualTo("Inicio de ruta");
            assertThat(entrega.getHistorial().getFirst().getFechaHora()).isNotNull();
        }
    }
}
