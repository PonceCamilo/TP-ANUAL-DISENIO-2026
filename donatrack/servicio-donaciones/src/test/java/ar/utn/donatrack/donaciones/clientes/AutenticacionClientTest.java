package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.config.PropiedadesDePrueba;
import ar.utn.donatrack.donaciones.salida.BandejaDeSalida;
import ar.utn.donatrack.donaciones.salida.Destinos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Qué deja AutenticacionClient en la bandeja; el envío HTTP se prueba en EnviadorAutenticacionTest. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutenticacionClient: avisos de cuentas para los importados por CSV")
class AutenticacionClientTest {

    @Mock
    private BandejaDeSalida salida;

    @Captor
    private ArgumentCaptor<Object> contenido;

    private static List<AutenticacionClient.DonanteImportado> personas(int cantidad) {
        return IntStream.range(0, cantidad)
                .mapToObj(i -> new AutenticacionClient.DonanteImportado(
                        UUID.randomUUID(), "persona" + i + "@mail.com", "Persona " + i, "HUMANA"))
                .toList();
    }

    @Test
    @DisplayName("Apagado (por defecto) no encola nada")
    void apagadoNoEncola() {
        AutenticacionClient cliente = new AutenticacionClient(salida, PropiedadesDePrueba.porDefecto());

        cliente.notificarImportados(personas(3));

        assertThat(cliente.avisaImportados()).isFalse();
        verify(salida, never()).encolar(anyString(), any());
    }

    @Test
    @DisplayName("Encendido, encola un aviso cada personasPorAviso personas, sin perder ninguna")
    void encolaPorTandas() {
        AutenticacionClient cliente = new AutenticacionClient(salida, PropiedadesDePrueba.conAvisoDeCuentas(2));
        List<AutenticacionClient.DonanteImportado> cinco = personas(5);

        cliente.notificarImportados(cinco);

        verify(salida, times(3)).encolar(eq(Destinos.AUTENTICACION_CUENTAS_IMPORTADAS), contenido.capture());
        List<AutenticacionClient.DonanteImportado> avisadas = contenido.getAllValues().stream()
                .map(aviso -> (AutenticacionClient.CuentasImportadas) aviso)
                .flatMap(aviso -> aviso.donantes().stream())
                .toList();
        assertThat(avisadas).containsExactlyElementsOf(cinco);
        assertThat(((AutenticacionClient.CuentasImportadas) contenido.getAllValues().getLast()).donantes()).hasSize(1);
    }

    @Test
    @DisplayName("Sin personas no encola nada")
    void sinPersonas() {
        AutenticacionClient cliente = new AutenticacionClient(salida, PropiedadesDePrueba.conAvisoDeCuentas(50));

        cliente.notificarImportados(List.of());

        verify(salida, never()).encolar(anyString(), any());
    }
}
