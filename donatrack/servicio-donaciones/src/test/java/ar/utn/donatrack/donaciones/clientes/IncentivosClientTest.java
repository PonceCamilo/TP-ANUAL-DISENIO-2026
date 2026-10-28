package ar.utn.donatrack.donaciones.clientes;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.salida.BandejaDeSalida;
import ar.utn.donatrack.donaciones.salida.Destinos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/** Qué deja IncentivosClient en la bandeja; el envío HTTP se prueba en EnviadorIncentivosTest. */
@ExtendWith(MockitoExtension.class)
@DisplayName("IncentivosClient: avisos al servicio de incentivos")
class IncentivosClientTest {

    @Mock
    private BandejaDeSalida salida;

    @InjectMocks
    private IncentivosClient cliente;

    @Captor
    private ArgumentCaptor<Object> contenido;

    @Test
    @DisplayName("La donación exitosa se informa con la fecha de entrega, no con la de registro")
    void donacionExitosaConFechaDeEntrega() {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(UUID.randomUUID());
        donacion.setFechaDonacion(LocalDateTime.of(2026, 1, 10, 9, 0));
        donacion.getBienes().add(BienGenerico.builder().subcategoria(new Subcategoria("arroz"))
                .descripcion("arroz").cantidad(3).unidad("kg").build());

        cliente.notificarDonacionExitosa(donacion, "ana@mail.com", "WHATSAPP", LocalDateTime.of(2026, 3, 15, 14, 30));

        verify(salida).encolar(eq(Destinos.INCENTIVOS_DONACION_EXITOSA), contenido.capture());
        IncentivosClient.SolicitudIncentivo solicitud = (IncentivosClient.SolicitudIncentivo) contenido.getValue();
        assertThat(solicitud.fecha()).isEqualTo(LocalDateTime.of(2026, 3, 15, 14, 30));
        assertThat(solicitud.destinatario()).isEqualTo("ana@mail.com");
        assertThat(solicitud.medio()).isEqualTo("WHATSAPP");
        assertThat(solicitud.cantidadBienes()).isEqualTo(3);
        assertThat(solicitud.categoriasDonadas()).containsExactly("arroz");
    }

    @Test
    @DisplayName("El registro de una donación usa los mismos campos que espera incentivos")
    void donacionRegistrada() {
        UUID donante = UUID.randomUUID();

        cliente.notificarDonacionRegistrada(donante, "ana@mail.com", "EMAIL", 2, List.of("arroz", "ropa"),
                LocalDateTime.of(2026, 3, 1, 10, 0));

        verify(salida).encolar(Destinos.INCENTIVOS_DONACION_REGISTRADA, new IncentivosClient.SolicitudIncentivo(
                donante, "ana@mail.com", "EMAIL", 2, List.of("arroz", "ropa"), LocalDateTime.of(2026, 3, 1, 10, 0), null));
    }
}
