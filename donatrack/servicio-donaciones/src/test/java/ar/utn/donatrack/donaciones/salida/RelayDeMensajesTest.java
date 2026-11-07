package ar.utn.donatrack.donaciones.salida;

import ar.utn.donatrack.donaciones.config.PropiedadesDePrueba;
import ar.utn.donatrack.donaciones.repositories.jpa.MensajeSalienteJpaRepository;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RelayDeMensajes - envía la bandeja de salida y registra cada resultado")
class RelayDeMensajesTest {

    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(30);

    private final MensajeSalienteJpaRepository mensajes = mock(MensajeSalienteJpaRepository.class);
    /** Lo que salió, en orden. Cada destino se envía en su propio hilo. */
    private final List<String> enviados = new CopyOnWriteArrayList<>();
    private RelayDeMensajes relay;

    /** Un enviador de prueba para un destino, que falla con los payloads que dicen "falla". */
    private EnviadorDeMensajes enviadorPara(String destino) {
        return new EnviadorDeMensajes() {
            @Override
            public Set<String> destinos() {
                return Set.of(destino);
            }

            @Override
            public void enviar(String destino, String payload) {
                if (payload.contains("falla")) {
                    throw new IllegalStateException("servicio caído");
                }
                enviados.add(payload);
            }
        };
    }

    private RelayDeMensajes relayCon(EnviadorDeMensajes... enviadores) {
        return new RelayDeMensajes(mensajes, List.of(enviadores), TransactionOperations.withoutTransaction(),
                PropiedadesDePrueba.porDefecto());
    }

    @BeforeEach
    void armar() {
        relay = relayCon(enviadorPara(Destinos.NOTIFICACION), enviadorPara(Destinos.INCENTIVOS_DONACION_REGISTRADA));
    }

    @AfterEach
    void restablecerReloj() {
        FechaHoraArgentina.restablecerReloj();
    }

    private MensajeSaliente pendiente(String destino, String payload) {
        MensajeSaliente mensaje = MensajeSaliente.nuevo(destino, payload, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(mensajes.findById(mensaje.getId())).thenReturn(Optional.of(mensaje));
        return mensaje;
    }

    /**
     * Deja listos los mensajes indicados. Como la base, cada lectura devuelve los
     * que siguen pendientes de ese destino, en orden y de a lo que pida la página.
     */
    private void listos(List<MensajeSaliente> pendientes) {
        Map<String, List<MensajeSaliente>> porDestino = new TreeMap<>();
        pendientes.forEach(m -> porDestino.computeIfAbsent(m.getDestino(), d -> new ArrayList<>()).add(m));
        when(mensajes.buscarDestinosConListos(eq(EstadoMensaje.PENDIENTE), any()))
                .thenReturn(List.copyOf(porDestino.keySet()));
        porDestino.forEach((destino, lista) ->
                when(mensajes.buscarListos(eq(EstadoMensaje.PENDIENTE), eq(destino), any(), any()))
                        .thenAnswer(pedido -> lista.stream()
                                .filter(m -> m.getEstado() == EstadoMensaje.PENDIENTE && m.getIntentos() == 0)
                                .limit(pedido.<Pageable>getArgument(3).getPageSize())
                                .toList()));
    }

    private void listos(MensajeSaliente... pendientes) {
        listos(List.of(pendientes));
    }

    @Test
    @DisplayName("Envía los listos y los marca enviados; el que falla queda para reintentar y los que siguen en su destino esperan a la corrida siguiente")
    void enviaYRegistra() {
        MensajeSaliente mal = pendiente(Destinos.NOTIFICACION, "{\"falla\":1}");
        MensajeSaliente despues = pendiente(Destinos.NOTIFICACION, "{\"aviso\":2}");
        MensajeSaliente incentivos1 = pendiente(Destinos.INCENTIVOS_DONACION_REGISTRADA, "{\"incentivos\":1}");
        MensajeSaliente incentivos2 = pendiente(Destinos.INCENTIVOS_DONACION_REGISTRADA, "{\"incentivos\":2}");
        listos(mal, despues, incentivos1, incentivos2);

        int cantidad = relay.enviarPendientes(TIEMPO_MAXIMO);

        // La falla de Notificaciones no frena a Incentivos.
        assertThat(cantidad).isEqualTo(2);
        assertThat(enviados).containsExactly("{\"incentivos\":1}", "{\"incentivos\":2}");
        assertThat(incentivos2.getEstado()).isEqualTo(EstadoMensaje.ENVIADO);
        assertThat(mal.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(mal.getIntentos()).isEqualTo(1);
        assertThat(mal.getUltimoError()).isEqualTo("servicio caído");
        // El que seguía para Notificaciones no se intentó: no gasta un intento ni otro timeout.
        assertThat(despues.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(despues.getIntentos()).isZero();
    }

    @Test
    @DisplayName("Un destino colgado no demora a los demás: cada destino se envía en su propio hilo")
    void unDestinoColgadoNoDemoraALosDemas() {
        CountDownLatch avisosEnviados = new CountDownLatch(3);
        AtomicBoolean incentivosEsperoALosAvisos = new AtomicBoolean();
        EnviadorDeMensajes colgado = new EnviadorDeMensajes() {
            @Override
            public Set<String> destinos() {
                return Set.of(Destinos.INCENTIVOS_DONACION_REGISTRADA);
            }

            @Override
            public void enviar(String destino, String payload) throws InterruptedException {
                // No contesta hasta que salieron los tres avisos a Notificaciones.
                incentivosEsperoALosAvisos.set(avisosEnviados.await(5, TimeUnit.SECONDS));
                enviados.add(payload);
            }
        };
        EnviadorDeMensajes notificaciones = new EnviadorDeMensajes() {
            @Override
            public Set<String> destinos() {
                return Set.of(Destinos.NOTIFICACION);
            }

            @Override
            public void enviar(String destino, String payload) {
                enviados.add(payload);
                avisosEnviados.countDown();
            }
        };
        relay = relayCon(colgado, notificaciones);
        listos(pendiente(Destinos.INCENTIVOS_DONACION_REGISTRADA, "{\"incentivos\":1}"),
                pendiente(Destinos.NOTIFICACION, "{\"aviso\":1}"),
                pendiente(Destinos.NOTIFICACION, "{\"aviso\":2}"),
                pendiente(Destinos.NOTIFICACION, "{\"aviso\":3}"));

        assertThat(relay.enviarPendientes(TIEMPO_MAXIMO)).isEqualTo(4);

        assertThat(incentivosEsperoALosAvisos).isTrue();
        assertThat(enviados).containsExactly("{\"aviso\":1}", "{\"aviso\":2}", "{\"aviso\":3}", "{\"incentivos\":1}");
    }

    @Test
    @DisplayName("Un destino con muchos pendientes sigue leyendo lotes hasta terminarlos, dentro del tiempo de la corrida")
    void vaciaVariosLotes() {
        List<MensajeSaliente> pendientes = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            pendientes.add(pendiente(Destinos.NOTIFICACION, "{\"aviso\":" + i + "}"));
        }
        listos(pendientes);

        // El lote es de 50: salen los 120 en la misma corrida.
        assertThat(relay.enviarPendientes(TIEMPO_MAXIMO)).isEqualTo(120);
        assertThat(pendientes).allSatisfy(m -> assertThat(m.getEstado()).isEqualTo(EstadoMensaje.ENVIADO));
        assertThat(enviados.getFirst()).isEqualTo("{\"aviso\":1}");
        assertThat(enviados.getLast()).isEqualTo("{\"aviso\":120}");
    }

    @Test
    @DisplayName("Un destino sin enviador cuenta como falla (no se pierde ni se da por enviado)")
    void destinoDesconocido() {
        MensajeSaliente huerfano = pendiente("OTRO_SERVICIO", "{}");
        listos(huerfano);

        assertThat(relay.enviarPendientes(TIEMPO_MAXIMO)).isZero();
        assertThat(huerfano.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(huerfano.getUltimoError()).contains("OTRO_SERVICIO");
    }

    @Test
    @DisplayName("Al llegar al tiempo máximo deja de tomar mensajes: los que faltan siguen pendientes, sin contar como intento")
    void cortaAlLlegarAlTiempoMaximo() {
        FechaHoraArgentina.usarReloj(Clock.fixed(Instant.parse("2026-01-01T06:00:00Z"), FechaHoraArgentina.ZONA));
        // Un servicio lento: cada envío tarda 20 segundos.
        relay = relayCon(new EnviadorDeMensajes() {
            @Override
            public Set<String> destinos() {
                return Set.of(Destinos.NOTIFICACION);
            }

            @Override
            public void enviar(String destino, String payload) {
                FechaHoraArgentina.usarReloj(Clock.offset(FechaHoraArgentina.reloj(), Duration.ofSeconds(20)));
                enviados.add(payload);
            }
        });
        MensajeSaliente primero = pendiente(Destinos.NOTIFICACION, "{\"n\":1}");
        MensajeSaliente segundo = pendiente(Destinos.NOTIFICACION, "{\"n\":2}");
        MensajeSaliente tercero = pendiente(Destinos.NOTIFICACION, "{\"n\":3}");
        listos(primero, segundo, tercero);

        // El segundo arranca a los 20 s, antes del corte de 30; el tercero arrancaría a los 40.
        assertThat(relay.enviarPendientes(TIEMPO_MAXIMO)).isEqualTo(2);

        assertThat(enviados).containsExactly("{\"n\":1}", "{\"n\":2}");
        assertThat(segundo.getEstado()).isEqualTo(EstadoMensaje.ENVIADO);
        assertThat(tercero.getEstado()).isEqualTo(EstadoMensaje.PENDIENTE);
        assertThat(tercero.getIntentos()).isZero();
        assertThat(tercero.getUltimoError()).isNull();
    }

    @Test
    @DisplayName("Dos enviadores para el mismo destino se rechazan al armar el relay")
    void destinosRepetidos() {
        EnviadorDeMensajes enviador = enviadorPara(Destinos.NOTIFICACION);
        assertThatThrownBy(() -> relayCon(enviador, enviador))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Destinos.NOTIFICACION);
    }
}
