package ar.utn.donatrack.donaciones.models.estado;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.comunes.EstadoDesconocidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import ar.utn.donatrack.donaciones.models.donacion.estado.EnDepositoState;
import ar.utn.donatrack.donaciones.models.donacion.estado.EnTrasladoState;
import ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion;
import ar.utn.donatrack.donaciones.models.donante.estado.ActivoState;
import ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El mecanismo común de los estados (Estado y CatalogoDeEstados), probado con
 * estados reales de las dos jerarquías.
 */
@DisplayName("Estado: base común de los estados de donación y de donante")
class EstadoTest {

    @Test
    @DisplayName("Cada estado declara sus destinos y la base rechaza los demás")
    void destinosYTransicionIlegal() {
        EnDepositoState enDeposito = new EnDepositoState();

        assertThat(enDeposito.transicionarA(EstadosDonacion.ASIGNACION_REALIZADA, null).nombre())
                .isEqualTo(EstadosDonacion.ASIGNACION_REALIZADA);
        assertThat(enDeposito.transicionarA(EstadosDonacion.VENCIDA, null).nombre()).isEqualTo(EstadosDonacion.VENCIDA);
        assertThatThrownBy(() -> enDeposito.transicionarA(EstadosDonacion.ENTREGADA, null))
                .isInstanceOf(TransicionDeEstadoIlegalException.class);
    }

    @Test
    @DisplayName("Las reglas propias de un estado corren antes de la transición")
    void reglasPropias() {
        assertThatThrownBy(() -> new EnTrasladoState().transicionarA(EstadosDonacion.ENTREGA_FALLIDA, " "))
                .isInstanceOf(FaltaJustificacionException.class);
        assertThatThrownBy(() -> new ActivoState().transicionarA(EstadosDonante.BLOQUEADO, null))
                .isInstanceOf(FaltaJustificacionException.class);
        assertThatThrownBy(() -> new ActivoState().transicionarA(EstadosDonante.ACTIVO, null))
                .isInstanceOf(PersonaConMismoEstadoException.class);
    }

    @Test
    @DisplayName("Dos instancias del mismo estado son iguales: los estados no tienen otros datos")
    void igualdad() {
        assertThat(new EnDepositoState()).isEqualTo(EstadosDonacion.inicial()).hasSameHashCodeAs(new EnDepositoState());
        assertThat(EstadosDonante.desde(EstadosDonante.ACTIVO)).isEqualTo(new ActivoState());
        assertThat(new EnDepositoState()).hasToString(EstadosDonacion.EN_DEPOSITO);
    }

    @Test
    @DisplayName("Un nombre que no está en el catálogo (o nulo) es un dato corrupto: EstadoDesconocido")
    void nombreDesconocido() {
        assertThatThrownBy(() -> EstadosDonacion.desde("PERDIDA")).isInstanceOf(EstadoDesconocidoException.class);
        assertThatThrownBy(() -> EstadosDonante.desde(null)).isInstanceOf(EstadoDesconocidoException.class);
    }
}
