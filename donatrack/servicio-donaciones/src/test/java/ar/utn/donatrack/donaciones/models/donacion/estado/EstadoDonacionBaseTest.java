package ar.utn.donatrack.donaciones.models.donacion.estado;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.CambioEstadoDonacionIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionDonacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del ciclo de vida de una donación, modelado con el patrón State.
 *
 * El diagrama de estados del enunciado (Entrega 2) define este recorrido:
 *
 *   EN_DEPOSITO -> ASIGNACION_REALIZADA -> LISTA_PARA_ENTREGAR -> EN_TRASLADO -> ENTREGADA
 *                                                                            -> ENTREGA_FALLIDA -> EN_DEPOSITO
 *   EN_DEPOSITO -> VENCIDA
 *
 * Estas pruebas verifican tanto el camino feliz como que no existan atajos:
 * la trazabilidad exigida por el enunciado depende de que no se pueda saltear
 * ningún paso del circuito.
 */
@DisplayName("EstadoDonacionBase - máquina de estados de la donación")
class EstadoDonacionBaseTest {

    @Nested
    @DisplayName("Recorrido completo del camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("EN_DEPOSITO -> ASIGNACION_REALIZADA cuando el algoritmo asigna una entidad")
        void enDepositoAAsignacionRealizada() {
            EstadoDonacionBase resultado = new EnDepositoState()
                    .transicionarA("ASIGNACION_REALIZADA", null);

            assertThat(resultado).isInstanceOf(AsignacionRealizadaState.class);
            assertThat(resultado.nombre()).isEqualTo("ASIGNACION_REALIZADA");
        }

        @Test
        @DisplayName("ASIGNACION_REALIZADA -> LISTA_PARA_ENTREGAR cuando se planifica una ruta que la incluye")
        void asignacionRealizadaAListaParaEntregar() {
            EstadoDonacionBase resultado = new AsignacionRealizadaState()
                    .transicionarA("LISTA_PARA_ENTREGAR", null);

            assertThat(resultado).isInstanceOf(ListaParaEntregarState.class);
        }

        @Test
        @DisplayName("LISTA_PARA_ENTREGAR -> EN_TRASLADO cuando el chofer inicia el recorrido")
        void listaParaEntregarAEnTraslado() {
            EstadoDonacionBase resultado = new ListaParaEntregarState()
                    .transicionarA("EN_TRASLADO", null);

            assertThat(resultado).isInstanceOf(EnTrasladoState.class);
        }

        @Test
        @DisplayName("EN_TRASLADO -> ENTREGADA cuando la entidad confirma la recepción")
        void enTrasladoAEntregada() {
            EstadoDonacionBase resultado = new EnTrasladoState()
                    .transicionarA("ENTREGADA", null);

            assertThat(resultado).isInstanceOf(EntregadaState.class);
        }

        @Test
        @DisplayName("El circuito completo encadenado llega a ENTREGADA")
        void circuitoCompletoEncadenado() {
            // Se recorre el ciclo de vida entero tal como ocurre en producción,
            // para verificar que cada estado enlaza correctamente con el siguiente.
            EstadoDonacionBase estado = new EnDepositoState();

            estado = estado.transicionarA("ASIGNACION_REALIZADA", null);
            estado = estado.transicionarA("LISTA_PARA_ENTREGAR", null);
            estado = estado.transicionarA("EN_TRASLADO", null);
            estado = estado.transicionarA("ENTREGADA", null);

            assertThat(estado.nombre()).isEqualTo("ENTREGADA");
        }
    }

    @Nested
    @DisplayName("Camino alternativo: entrega fallida y reingreso al depósito")
    class EntregaFallida {

        @Test
        @DisplayName("EN_TRASLADO -> ENTREGA_FALLIDA exige justificación (enunciado Entrega 2)")
        void entregaFallidaExigeJustificacion() {
            // El enunciado pide explícitamente registrar por qué no se pudo entregar
            // (ej: "Tocamos timbre pero nadie respondió").
            EstadoDonacionBase resultado = new EnTrasladoState()
                    .transicionarA("ENTREGA_FALLIDA", "Tocamos timbre pero nadie respondió");

            assertThat(resultado).isInstanceOf(EntregaFallidaState.class);
        }

        @Test
        @DisplayName("ENTREGA_FALLIDA sin justificación (null) se rechaza")
        void entregaFallidaSinJustificacionNull() {
            assertThatThrownBy(() -> new EnTrasladoState().transicionarA("ENTREGA_FALLIDA", null))
                    .isInstanceOf(FaltaJustificacionDonacionException.class);
        }

        @Test
        @DisplayName("ENTREGA_FALLIDA con justificación en blanco se rechaza")
        void entregaFallidaConJustificacionEnBlanco() {
            assertThatThrownBy(() -> new EnTrasladoState().transicionarA("ENTREGA_FALLIDA", "  "))
                    .isInstanceOf(FaltaJustificacionDonacionException.class);
        }

        @Test
        @DisplayName("La entrega exitosa NO exige justificación, solo la fallida")
        void entregaExitosaNoExigeJustificacion() {
            // Se verifica que la validación agregada en EnTrasladoState no se
            // aplique por error a la transición a ENTREGADA.
            EstadoDonacionBase resultado = new EnTrasladoState().transicionarA("ENTREGADA", null);

            assertThat(resultado).isInstanceOf(EntregadaState.class);
        }

        @Test
        @DisplayName("ENTREGA_FALLIDA -> EN_DEPOSITO: la donación vuelve al depósito para replanificarse")
        void entregaFallidaVuelveAlDeposito() {
            EstadoDonacionBase resultado = new EntregaFallidaState()
                    .transicionarA("EN_DEPOSITO", null);

            assertThat(resultado).isInstanceOf(EnDepositoState.class);
        }

        @Test
        @DisplayName("ENTREGA_FALLIDA -> EN_TRASLADO: el proveedor reintenta la entrega sin pasar por el depósito")
        void entregaFallidaPermiteReintento() {
            // Los dos proveedores de logística pueden reintentar una entrega
            // fallida (el externo permite FALLIDO -> EN_CAMINO). Cuando eso pasa
            // llega otro inicio-ruta para una donación en ENTREGA_FALLIDA; sin
            // esta transición el callback fallaría y la donación quedaría
            // desincronizada respecto de lo que el proveedor cree que pasa.
            EstadoDonacionBase resultado = new EntregaFallidaState()
                    .transicionarA("EN_TRASLADO", null);

            assertThat(resultado).isInstanceOf(EnTrasladoState.class);
        }

        @Test
        @DisplayName("Un reintento puede terminar entregado, cerrando el circuito")
        void reintentoPuedeTerminarEntregado() {
            EstadoDonacionBase estado = new EntregaFallidaState()
                    .transicionarA("EN_TRASLADO", null)
                    .transicionarA("ENTREGADA", null);

            assertThat(estado.nombre()).isEqualTo("ENTREGADA");
        }
    }

    @Nested
    @DisplayName("Vencimiento")
    class Vencimiento {

        @Test
        @DisplayName("EN_DEPOSITO -> VENCIDA: la administración marca la donación como vencida")
        void enDepositoAVencida() {
            EstadoDonacionBase resultado = new EnDepositoState().transicionarA("VENCIDA", null);

            assertThat(resultado).isInstanceOf(VencidaState.class);
        }
    }

    @Nested
    @DisplayName("Estados terminales")
    class EstadosTerminales {

        @Test
        @DisplayName("ENTREGADA es terminal: no admite ninguna transición de salida")
        void entregadaEsTerminal() {
            // Una vez entregada, la donación cerró su ciclo de vida.
            assertThatThrownBy(() -> new EntregadaState().transicionarA("EN_DEPOSITO", null))
                    .isInstanceOf(CambioEstadoDonacionIlegalException.class);

            assertThatThrownBy(() -> new EntregadaState().transicionarA("EN_TRASLADO", null))
                    .isInstanceOf(CambioEstadoDonacionIlegalException.class);
        }

        @Test
        @DisplayName("VENCIDA es terminal: la donación ya no puede reutilizarse")
        void vencidaEsTerminal() {
            assertThatThrownBy(() -> new VencidaState().transicionarA("EN_DEPOSITO", null))
                    .isInstanceOf(CambioEstadoDonacionIlegalException.class);
        }
    }

    @Nested
    @DisplayName("Transiciones ilegales: no se puede saltear pasos del circuito")
    class TransicionesIlegales {

        @Test
        @DisplayName("EN_DEPOSITO no puede ir directo a EN_TRASLADO sin ser asignada ni planificada")
        void noSePuedeSaltearAsignacionYPlanificacion() {
            assertThatThrownBy(() -> new EnDepositoState().transicionarA("EN_TRASLADO", null))
                    .isInstanceOf(CambioEstadoDonacionIlegalException.class)
                    .hasMessageContaining("EN_DEPOSITO");
        }

        @Test
        @DisplayName("EN_DEPOSITO no puede ir directo a ENTREGADA")
        void noSePuedeSaltearTodoElCircuito() {
            assertThatThrownBy(() -> new EnDepositoState().transicionarA("ENTREGADA", null))
                    .isInstanceOf(CambioEstadoDonacionIlegalException.class);
        }

        @Test
        @DisplayName("ASIGNACION_REALIZADA no puede volver a EN_DEPOSITO")
        void asignadaNoRetrocedeADeposito() {
            assertThatThrownBy(() -> new AsignacionRealizadaState().transicionarA("EN_DEPOSITO", null))
                    .isInstanceOf(CambioEstadoDonacionIlegalException.class);
        }

        @Test
        @DisplayName("Un estado destino inexistente se rechaza como transición ilegal")
        void estadoDestinoInexistente() {
            assertThatThrownBy(() -> new EnDepositoState().transicionarA("ESTADO_INVENTADO", null))
                    .isInstanceOf(CambioEstadoDonacionIlegalException.class);
        }
    }

    @Nested
    @DisplayName("Identidad de los estados")
    class IdentidadDeEstados {

        @Test
        @DisplayName("Cada estado expone el nombre con el que viaja por la API")
        void nombreDeCadaEstado() {
            assertThat(new EnDepositoState().nombre()).isEqualTo("EN_DEPOSITO");
            assertThat(new AsignacionRealizadaState().nombre()).isEqualTo("ASIGNACION_REALIZADA");
            assertThat(new ListaParaEntregarState().nombre()).isEqualTo("LISTA_PARA_ENTREGAR");
            assertThat(new EnTrasladoState().nombre()).isEqualTo("EN_TRASLADO");
            assertThat(new EntregadaState().nombre()).isEqualTo("ENTREGADA");
            assertThat(new EntregaFallidaState().nombre()).isEqualTo("ENTREGA_FALLIDA");
            assertThat(new VencidaState().nombre()).isEqualTo("VENCIDA");
        }
    }
}
