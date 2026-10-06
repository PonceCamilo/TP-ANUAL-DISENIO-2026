package ar.utn.donatrack.logisticaexterna.models;

import ar.utn.donatrack.logisticaexterna.exceptions.TransicionEnvioIlegalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Envio - ciclo de vida")
class EnvioTest {

    private Envio nuevoEnvio() {
        return new Envio("EXT-1", "don-1", "Comedor", "Medrano 951", LocalDate.now());
    }

    @Test
    @DisplayName("Arranca ACEPTADO con su historial inicial")
    void estadoInicial() {
        Envio envio = nuevoEnvio();

        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.ACEPTADO);
        assertThat(envio.getHistorial()).hasSize(1);
    }

    @Test
    @DisplayName("ACEPTADO -> EN_CAMINO -> ENTREGADO registra vehículo e historial")
    void recorridoExitoso() {
        Envio envio = nuevoEnvio();

        envio.despachar("AB123CD");
        envio.entregar();

        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.ENTREGADO);
        assertThat(envio.getVehiculo()).isEqualTo("AB123CD");
        assertThat(envio.getHistorial()).extracting(CambioEstadoEnvio::estado)
                .containsExactly(EstadoEnvio.ACEPTADO, EstadoEnvio.EN_CAMINO, EstadoEnvio.ENTREGADO);
    }

    @Test
    @DisplayName("Un envío FALLIDO se puede reintentar y limpia el motivo")
    void reintentoTrasFallo() {
        Envio envio = nuevoEnvio();
        envio.despachar("AB123CD");
        envio.fallar("Destinatario ausente");

        assertThat(envio.getMotivoFallo()).isEqualTo("Destinatario ausente");

        envio.despachar("ZZ999ZZ");

        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.EN_CAMINO);
        assertThat(envio.getMotivoFallo()).isNull();
        assertThat(envio.getVehiculo()).isEqualTo("ZZ999ZZ");
    }

    @Test
    @DisplayName("No se puede entregar un envío que no salió")
    void entregarSinDespachar() {
        Envio envio = nuevoEnvio();

        assertThatThrownBy(envio::entregar).isInstanceOf(TransicionEnvioIlegalException.class);
        assertThat(envio.getEstado()).isEqualTo(EstadoEnvio.ACEPTADO);
    }

    @Test
    @DisplayName("ENTREGADO es terminal")
    void entregadoEsTerminal() {
        Envio envio = nuevoEnvio();
        envio.despachar("AB123CD");
        envio.entregar();

        assertThatThrownBy(() -> envio.despachar("AB123CD")).isInstanceOf(TransicionEnvioIlegalException.class);
        assertThatThrownBy(() -> envio.fallar("x")).isInstanceOf(TransicionEnvioIlegalException.class);
    }
}
