package ar.utn.donatrack.brokerlogistica.services;

import ar.utn.donatrack.brokerlogistica.adapters.ProveedorLogisticaAdapter;
import ar.utn.donatrack.brokerlogistica.dtos.DireccionDTO;
import ar.utn.donatrack.brokerlogistica.dtos.DonacionEnvioDTO;
import ar.utn.donatrack.brokerlogistica.dtos.EnvioAsignadoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.ProveedorEstadoDTO;
import ar.utn.donatrack.brokerlogistica.dtos.ResultadoEnvioResponse;
import ar.utn.donatrack.brokerlogistica.dtos.SolicitudEnvioRequest;
import ar.utn.donatrack.brokerlogistica.exceptions.ProveedorNoDisponibleException;
import ar.utn.donatrack.brokerlogistica.exceptions.SinProveedorDisponibleException;
import ar.utn.donatrack.brokerlogistica.repositories.RegistroEnviosRepository;
import ar.utn.donatrack.brokerlogistica.seleccion.SeleccionPorPrioridad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Se usan el criterio por prioridad y el registro reales; solo se mockean los
 * adapters, que es donde está el HTTP.
 */
@DisplayName("BrokerEnviosService - selección de proveedor y fallback")
class BrokerEnviosServiceTest {

    private ProveedorLogisticaAdapter donatrack;
    private ProveedorLogisticaAdapter externa;
    private RegistroEnviosRepository registro;
    private BrokerEnviosService servicio;

    private final DonacionEnvioDTO donacion = new DonacionEnvioDTO(
            UUID.randomUUID(), UUID.randomUUID(), "Comedor Los Pibes",
            new DireccionDTO("Medrano", 951, "CABA", "Buenos Aires", "C1179"));

    @BeforeEach
    void prepararEscenario() {
        donatrack = mock(ProveedorLogisticaAdapter.class);
        externa = mock(ProveedorLogisticaAdapter.class);
        when(donatrack.nombre()).thenReturn("DONATRACK");
        when(externa.nombre()).thenReturn("EXTERNA");
        registro = new RegistroEnviosRepository();
        servicio = new BrokerEnviosService(
                List.of(externa, donatrack),
                new SeleccionPorPrioridad(List.of("DONATRACK", "EXTERNA")),
                registro);
    }

    private List<EnvioAsignadoDTO> asignadoA(String proveedor) {
        return List.of(new EnvioAsignadoDTO(donacion.idDonacion(), proveedor, "seg-1", LocalDateTime.now()));
    }

    private SolicitudEnvioRequest solicitud(String proveedor) {
        return new SolicitudEnvioRequest(proveedor, List.of(donacion));
    }

    @Test
    @DisplayName("Sin proveedor pedido usa el de mayor prioridad y registra el envío")
    void usaElPrioritario() {
        when(donatrack.disponible()).thenReturn(true);
        when(donatrack.solicitarEnvios(anyList())).thenReturn(asignadoA("DONATRACK"));

        ResultadoEnvioResponse resultado = servicio.solicitarEnvio(solicitud(null));

        assertThat(resultado.proveedor()).isEqualTo("DONATRACK");
        assertThat(resultado.proveedoresDescartados()).isEmpty();
        assertThat(servicio.consultarEnvio(donacion.idDonacion()).proveedor()).isEqualTo("DONATRACK");
        verify(externa, never()).solicitarEnvios(anyList());
    }

    @Test
    @DisplayName("Si el prioritario no pasa el health check, hace fallback al siguiente sin llamarlo")
    void fallbackPorHealthCheck() {
        when(donatrack.disponible()).thenReturn(false);
        when(externa.disponible()).thenReturn(true);
        when(externa.solicitarEnvios(anyList())).thenReturn(asignadoA("EXTERNA"));

        ResultadoEnvioResponse resultado = servicio.solicitarEnvio(solicitud(null));

        assertThat(resultado.proveedor()).isEqualTo("EXTERNA");
        assertThat(resultado.proveedoresDescartados()).containsExactly("DONATRACK");
        verify(donatrack, never()).solicitarEnvios(anyList());
    }

    @Test
    @DisplayName("Si el prioritario falla al pedirle el envío, hace fallback al siguiente")
    void fallbackPorFalloEnElPedido() {
        when(donatrack.disponible()).thenReturn(true);
        when(donatrack.solicitarEnvios(anyList()))
                .thenThrow(new ProveedorNoDisponibleException("DONATRACK", new RuntimeException("503")));
        when(externa.disponible()).thenReturn(true);
        when(externa.solicitarEnvios(anyList())).thenReturn(asignadoA("EXTERNA"));

        ResultadoEnvioResponse resultado = servicio.solicitarEnvio(solicitud(null));

        assertThat(resultado.proveedor()).isEqualTo("EXTERNA");
        assertThat(resultado.proveedoresDescartados()).containsExactly("DONATRACK");
    }

    @Test
    @DisplayName("Con proveedor pedido y caído no hace fallback: 503")
    void proveedorPedidoCaido() {
        when(externa.disponible()).thenReturn(false);

        assertThatThrownBy(() -> servicio.solicitarEnvio(solicitud("EXTERNA")))
                .isInstanceOf(SinProveedorDisponibleException.class);
        verify(donatrack, never()).disponible();
    }

    @Test
    @DisplayName("Si ninguno está disponible lanza SinProveedorDisponibleException y no registra nada")
    void ningunoDisponible() {
        when(donatrack.disponible()).thenReturn(false);
        when(externa.disponible()).thenReturn(false);

        assertThatThrownBy(() -> servicio.solicitarEnvio(solicitud(null)))
                .isInstanceOf(SinProveedorDisponibleException.class)
                .hasMessageContaining("DONATRACK")
                .hasMessageContaining("EXTERNA");
        assertThat(registro.buscarPorDonacion(donacion.idDonacion())).isEmpty();
    }

    @Test
    @DisplayName("El estado de proveedores sale en orden de prioridad con su disponibilidad")
    void estadoProveedores() {
        when(donatrack.disponible()).thenReturn(false);
        when(externa.disponible()).thenReturn(true);

        assertThat(servicio.estadoProveedores()).containsExactly(
                new ProveedorEstadoDTO("DONATRACK", 1, false),
                new ProveedorEstadoDTO("EXTERNA", 2, true));
    }
}
