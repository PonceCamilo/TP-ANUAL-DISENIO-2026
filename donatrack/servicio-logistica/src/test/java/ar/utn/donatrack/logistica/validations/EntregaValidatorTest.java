package ar.utn.donatrack.logistica.validations;

import ar.utn.donatrack.logistica.exceptions.TransicionEntregaIlegalException;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del ciclo de vida de una entrega, validado de forma explícita
 * (enum + switch, no jerarquía State como en Donaciones).
 *
 * Recorrido:
 *   LISTO_PARA_ENTREGAR -> EN_TRASLADO -> ENTREGADA
 *                                      -> NO_RECIBIDA -> LISTO_PARA_ENTREGAR
 *   ENTREGADA es terminal.
 */
@DisplayName("EntregaValidator - transiciones de estado de la entrega")
class EntregaValidatorTest {

    private final EntregaValidator validador = new EntregaValidator();

    @Nested
    @DisplayName("Recorrido válido")
    class RecorridoValido {

        @Test
        @DisplayName("LISTO_PARA_ENTREGAR -> EN_TRASLADO es válida (inicio de ruta)")
        void listoAEnTraslado() {
            assertThatCode(() -> validador.validarTransicion(
                    EstadoEntrega.LISTO_PARA_ENTREGAR, EstadoEntrega.EN_TRASLADO))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("EN_TRASLADO -> ENTREGADA es válida (confirmación de la entidad)")
        void enTrasladoAEntregada() {
            assertThatCode(() -> validador.validarTransicion(
                    EstadoEntrega.EN_TRASLADO, EstadoEntrega.ENTREGADA))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("EN_TRASLADO -> NO_RECIBIDA es válida")
        void enTrasladoANoRecibida() {
            assertThatCode(() -> validador.validarTransicion(
                    EstadoEntrega.EN_TRASLADO, EstadoEntrega.NO_RECIBIDA))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("NO_RECIBIDA -> LISTO_PARA_ENTREGAR es válida (regreso a depósito)")
        void noRecibidaAListo() {
            assertThatCode(() -> validador.validarTransicion(
                    EstadoEntrega.NO_RECIBIDA, EstadoEntrega.LISTO_PARA_ENTREGAR))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Estados terminales")
    class EstadosTerminales {

        @Test
        @DisplayName("ENTREGADA es terminal: cualquier transición es inválida")
        void entregadaEsTerminal() {
            assertThatThrownBy(() -> validador.validarTransicion(
                    EstadoEntrega.ENTREGADA, EstadoEntrega.LISTO_PARA_ENTREGAR))
                    .isInstanceOf(TransicionEntregaIlegalException.class);
        }
    }

    @Nested
    @DisplayName("Transiciones ilegales: no se puede saltear pasos")
    class TransicionesIlegales {

        @Test
        @DisplayName("LISTO_PARA_ENTREGAR -> ENTREGADA es inválida: no se puede saltear EN_TRASLADO")
        void noSePuedeSaltearEnTraslado() {
            assertThatThrownBy(() -> validador.validarTransicion(
                    EstadoEntrega.LISTO_PARA_ENTREGAR, EstadoEntrega.ENTREGADA))
                    .isInstanceOf(TransicionEntregaIlegalException.class)
                    .hasMessageContaining("LISTO_PARA_ENTREGAR");
        }

        @Test
        @DisplayName("EN_TRASLADO -> LISTO_PARA_ENTREGAR es inválida")
        void enTrasladoNoVuelveAListo() {
            assertThatThrownBy(() -> validador.validarTransicion(
                    EstadoEntrega.EN_TRASLADO, EstadoEntrega.LISTO_PARA_ENTREGAR))
                    .isInstanceOf(TransicionEntregaIlegalException.class);
        }

        @Test
        @DisplayName("NO_RECIBIDA -> ENTREGADA es inválida")
        void noRecibidaNoPasaAEntregada() {
            assertThatThrownBy(() -> validador.validarTransicion(
                    EstadoEntrega.NO_RECIBIDA, EstadoEntrega.ENTREGADA))
                    .isInstanceOf(TransicionEntregaIlegalException.class);
        }
    }
}
