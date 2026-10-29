package ar.utn.donatrack.donaciones.models.donante.estado;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests del ciclo de vida de una persona donante, modelado con el patrón State.
 *
 * El enunciado define tres estados posibles (ACTIVO, INACTIVO, BLOQUEADO) y este
 * modelo los implementa como clases separadas en lugar de un enum, de modo que
 * cada estado conoce sus propias transiciones válidas.
 *
 * Estas pruebas verifican las tres reglas de negocio que distinguen los errores
 * entre sí (cada una mapea a un código HTTP distinto en la API):
 *   - Transición a un estado no alcanzable desde el actual -> 422
 *   - Transición al mismo estado en el que ya se encuentra  -> 409
 *   - Bloqueo sin justificación                             -> 400
 */
@DisplayName("EstadoDonante - máquina de estados de la persona donante")
class EstadoDonanteTest {

    @Nested
    @DisplayName("Transiciones válidas")
    class TransicionesValidas {

        @Test
        @DisplayName("ACTIVO -> INACTIVO no requiere justificación")
        void activoAInactivo() {
            // Dar de baja a un donante es una acción reversible y sin impacto
            // punitivo, por eso no se le exige justificación.
            EstadoDonante resultado = new ActivoState().transicionarA("INACTIVO", null);

            assertThat(resultado).isInstanceOf(InactivoState.class);
            assertThat(resultado.nombre()).isEqualTo("INACTIVO");
        }

        @Test
        @DisplayName("ACTIVO -> BLOQUEADO con justificación")
        void activoABloqueadoConJustificacion() {
            EstadoDonante resultado = new ActivoState()
                    .transicionarA("BLOQUEADO", "Reportes de contenido inapropiado");

            assertThat(resultado).isInstanceOf(BloqueadoState.class);
        }

        @Test
        @DisplayName("INACTIVO -> ACTIVO permite reactivar a un donante dado de baja")
        void inactivoAActivo() {
            EstadoDonante resultado = new InactivoState().transicionarA("ACTIVO", null);

            assertThat(resultado).isInstanceOf(ActivoState.class);
        }

        @Test
        @DisplayName("BLOQUEADO -> ACTIVO permite desbloquear a un donante")
        void bloqueadoAActivo() {
            EstadoDonante resultado = new BloqueadoState().transicionarA("ACTIVO", null);

            assertThat(resultado).isInstanceOf(ActivoState.class);
        }
    }

    @Nested
    @DisplayName("Transiciones ilegales (se responden con 422)")
    class TransicionesIlegales {

        @Test
        @DisplayName("BLOQUEADO -> INACTIVO no está permitida: desde BLOQUEADO solo se puede volver a ACTIVO")
        void bloqueadoAInactivoEsIlegal() {
            assertThatThrownBy(() -> new BloqueadoState().transicionarA("INACTIVO", "cualquier motivo"))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class)
                    .hasMessageContaining("BLOQUEADO")
                    .hasMessageContaining("INACTIVO");
        }

        @Test
        @DisplayName("INACTIVO -> BLOQUEADO no está permitida: primero hay que reactivar al donante")
        void inactivoABloqueadoEsIlegal() {
            assertThatThrownBy(() -> new InactivoState().transicionarA("BLOQUEADO", "spam"))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);
        }

        @Test
        @DisplayName("Un estado destino inexistente se rechaza como transición ilegal")
        void estadoDestinoInexistente() {
            // Como el estado viaja por la API como String, un valor arbitrario
            // debe caer en el mismo camino que una transición no permitida.
            assertThatThrownBy(() -> new ActivoState().transicionarA("ESTADO_INVENTADO", null))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);
        }
    }

    @Nested
    @DisplayName("Mismo estado (se responde con 409)")
    class MismoEstado {

        @Test
        @DisplayName("ACTIVO -> ACTIVO se rechaza porque ya está en ese estado")
        void activoAActivo() {
            assertThatThrownBy(() -> new ActivoState().transicionarA("ACTIVO", null))
                    .isInstanceOf(PersonaConMismoEstadoException.class)
                    .hasMessageContaining("ACTIVO");
        }

        @Test
        @DisplayName("INACTIVO -> INACTIVO también se rechaza")
        void inactivoAInactivo() {
            assertThatThrownBy(() -> new InactivoState().transicionarA("INACTIVO", null))
                    .isInstanceOf(PersonaConMismoEstadoException.class);
        }

        @Test
        @DisplayName("La comprobación de mismo estado tiene prioridad sobre la de transición ilegal")
        void mismoEstadoTienePrioridad() {
            // BLOQUEADO -> BLOQUEADO podría interpretarse como transición ilegal
            // (no está en el mapa), pero el chequeo de "mismo estado" corre primero
            // para poder devolver 409 en lugar de 422.
            assertThatThrownBy(() -> new BloqueadoState().transicionarA("BLOQUEADO", "motivo"))
                    .isInstanceOf(PersonaConMismoEstadoException.class);
        }
    }

    @Nested
    @DisplayName("Justificación obligatoria al bloquear (se responde con 400)")
    class JustificacionObligatoria {

        @Test
        @DisplayName("Bloquear sin justificación (null) se rechaza")
        void bloquearSinJustificacionNull() {
            assertThatThrownBy(() -> new ActivoState().transicionarA("BLOQUEADO", null))
                    .isInstanceOf(FaltaJustificacionException.class)
                    .hasMessageContaining("justificación");
        }

        @Test
        @DisplayName("Bloquear con justificación vacía o en blanco también se rechaza")
        void bloquearConJustificacionEnBlanco() {
            assertThatThrownBy(() -> new ActivoState().transicionarA("BLOQUEADO", "   "))
                    .isInstanceOf(FaltaJustificacionException.class);
        }

        @Test
        @DisplayName("La justificación solo se exige al bloquear, no en las demás transiciones")
        void otrasTransicionesNoExigenJustificacion() {
            // Se comprueba explícitamente que la regla no se filtró a otras transiciones.
            assertThatCode(() -> new ActivoState().transicionarA("INACTIVO", null))
                    .doesNotThrowAnyException();
            assertThatCode(() -> new InactivoState().transicionarA("ACTIVO", null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Identidad de los estados")
    class IdentidadDeEstados {

        @Test
        @DisplayName("Cada estado expone su nombre, que es el valor que viaja por la API")
        void nombreDeCadaEstado() {
            assertThat(new ActivoState().nombre()).isEqualTo("ACTIVO");
            assertThat(new InactivoState().nombre()).isEqualTo("INACTIVO");
            assertThat(new BloqueadoState().nombre()).isEqualTo("BLOQUEADO");
        }

        @Test
        @DisplayName("Cada transición devuelve una instancia nueva, sin mutar el estado anterior")
        void transicionDevuelveInstanciaNueva() {
            // El patrón State usado es inmutable: transicionarA no modifica el
            // objeto receptor, devuelve el nuevo estado. Esto permite que la
            // donación/persona guarde el estado previo en su historial.
            EstadoDonante original = new ActivoState();
            EstadoDonante nuevo = original.transicionarA("INACTIVO", null);

            assertThat(nuevo).isNotSameAs(original);
            assertThat(original.nombre()).isEqualTo("ACTIVO");
        }
    }
}
