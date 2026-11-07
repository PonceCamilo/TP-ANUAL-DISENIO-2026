package ar.utn.donatrack.logistica.models.entrega;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la regla de replanificación derivada del motivo de fallo.
 *
 * El chofer solo informa el motivo: Logística decide si la donación puede
 * reintentarse. Ausencia / dirección incorrecta / rechazo sí; mercadería
 * rota, perdida o robada no.
 */
@DisplayName("MotivoFalloEntrega - derivación de replanificable")
class MotivoFalloEntregaTest {

    @Nested
    @DisplayName("Motivos replanificables")
    class Replanificables {

        @Test
        @DisplayName("ENTIDAD_AUSENTE, DIRECCION_INCORRECTA y RECHAZADA_POR_ENTIDAD se pueden reintentar")
        void sePuedenReintentar() {
            assertThat(MotivoFalloEntrega.ENTIDAD_AUSENTE.esReplanificable()).isTrue();
            assertThat(MotivoFalloEntrega.DIRECCION_INCORRECTA.esReplanificable()).isTrue();
            assertThat(MotivoFalloEntrega.RECHAZADA_POR_ENTIDAD.esReplanificable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Motivos no replanificables")
    class NoReplanificables {

        @Test
        @DisplayName("MERCADERIA_ROTA, MERCADERIA_PERDIDA y ROBO no se reintentan")
        void noSeReintentan() {
            assertThat(MotivoFalloEntrega.MERCADERIA_ROTA.esReplanificable()).isFalse();
            assertThat(MotivoFalloEntrega.MERCADERIA_PERDIDA.esReplanificable()).isFalse();
            assertThat(MotivoFalloEntrega.ROBO.esReplanificable()).isFalse();
        }
    }
}
