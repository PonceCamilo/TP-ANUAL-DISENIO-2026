package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.repositories.jpa.MensajeSalienteJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RelayDeMensajes - envía la bandeja de salida y registra cada resultado")
class RelayDeMensajesTest {

    private final MensajeSalienteJpaRepository mensajes = mock(MensajeSalienteJpaRepository.class);
    private final List<String> enviados = new ArrayList<>();
    private RelayDeMensajes relay;

    /** Un enviador de prueba que falla con los payloads que dicen "falla". */
    private final EnviadorDeMensajes enviador = new EnviadorDeMensajes() {
        @Override
        public Set<String> destinos() {
            return Set.of(Destinos.NOTIFICACION);
        }

        @Override
        public void enviar(String destino, String payload) {
            if (payload.contains("falla")) {
                throw new IllegalStateException("servicio caído");
            }
            enviados.add(payload);
        }
    };

    @BeforeEach
    void armar() {
        relay = new RelayDeMensajes(mensajes, List.of(enviador), TransactionOperations.withoutTransaction(),
                DonacionesProperties.porDefecto());
    }

    private MensajeSaliente pendiente(String destino, String payload) {
        MensajeSaliente mensaje = MensajeSaliente.nuevo(destino, payload, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(mensajes.findById(mensaje.getId())).thenReturn(Optional.of(mensaje));
        return mensaje;
    }

    @Test
    @DisplayName("Envía los listos y los marca enviados; el que falla queda pendiente para reintentar")
    void enviaYRegistra() {
        MensajeSaliente bien = pendiente(Destinos.NOTIFICACION, "{\"ok\":1}");
        MensajeSaliente mal = pendiente(Destinos.NOTIFICACION, "{\"falla\":1}");
        MensajeSaliente otro = pendiente(Destinos.NOTIFICACION, "{\"ok\":2}");
        when(mensajes.buscarListos(eq(EstadoMensaje.PENDIENTE), any(), any())).thenReturn(List.of(bien, mal, otro));

        int cantidad = relay.enviarPendientes();

        // Un mensaje que falla no frena a los que siguen.
        assertThat(cantidad).isEqualTo(2);
        assertThat(enviados).containsExactly("{\"ok\":1}", "{\"ok\":2}");
        assertThat(bien.getEstado()).isEqualTo(EstadoMensaje.ENVIADO);
        assertThat(mal.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(mal.getIntentos()).isEqualTo(1);
        assertThat(mal.getUltimoError()).isEqualTo("servicio caído");
    }

    @Test
    @DisplayName("Un destino sin enviador cuenta como falla (no se pierde ni se da por enviado)")
    void destinoDesconocido() {
        MensajeSaliente huerfano = pendiente("OTRO_SERVICIO", "{}");
        when(mensajes.buscarListos(eq(EstadoMensaje.PENDIENTE), any(), any())).thenReturn(List.of(huerfano));

        assertThat(relay.enviarPendientes()).isZero();
        assertThat(huerfano.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(huerfano.getUltimoError()).contains("OTRO_SERVICIO");
    }

    @Test
    @DisplayName("Dos enviadores para el mismo destino se rechazan al armar el relay")
    void destinosRepetidos() {
        assertThatThrownBy(() -> new RelayDeMensajes(mensajes, List.of(enviador, enviador),
                TransactionOperations.withoutTransaction(), DonacionesProperties.porDefecto()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Destinos.NOTIFICACION);
    }
}
