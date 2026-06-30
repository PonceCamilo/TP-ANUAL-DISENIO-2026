package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.ConfirmarEntregaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.NoRecibidaRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.exceptions.EntregaNoEncontradaException;
import ar.utn.donatrack.logistica.exceptions.TransicionEntregaIlegalException;
import ar.utn.donatrack.logistica.integracion.EntregaEventPublisher;
import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.validations.EntregaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

    @Mock
    private EntregaRepositoryInterface repositorio;

    @Mock
    private EntregaEventPublisher eventPublisher;

    private EntregaService service;

    @BeforeEach
    void setUp() {
        service = new EntregaService(repositorio, new EntregaValidator(), eventPublisher);
    }

    private Entrega entregaEnEstado(EstadoEntrega estado) {
        return Entrega.builder()
                .id(UUID.randomUUID())
                .idDonacion(UUID.randomUUID())
                .idEntidadBeneficiaria(UUID.randomUUID())
                .idDonante(UUID.randomUUID())
                .estado(estado)
                .build();
    }

    @Nested
    @DisplayName("confirmar()")
    class Confirmar {

        @Test
        @DisplayName("Entrega EN_TRASLADO pasa a ENTREGADA, guarda las fotos y publica ENTREGA_CONFIRMADA")
        void confirmaEntregaEnTraslado() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);

            ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO();
            dto.setFotosComprobante(List.of("foto1.jpg"));

            service.confirmar(entrega.getId(), dto);

            assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstado());
            assertTrue(entrega.getFotosComprobante().contains("foto1.jpg"));
            verify(repositorio).guardar(entrega);

            ArgumentCaptor<EntregaEvento> captor = ArgumentCaptor.forClass(EntregaEvento.class);
            verify(eventPublisher).publicar(captor.capture());
            assertEquals(TipoEventoLogistica.ENTREGA_CONFIRMADA, captor.getValue().getTipo());
            assertEquals(entrega.getIdDonante(), captor.getValue().getIdDonante());
        }

        @Test
        @DisplayName("Entrega PENDIENTE no puede confirmarse directamente")
        void noPermiteConfirmarSinIniciarRuta() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.PENDIENTE);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);

            ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO();
            dto.setFotosComprobante(List.of("foto1.jpg"));

            assertThrows(TransicionEntregaIlegalException.class, () -> service.confirmar(entrega.getId(), dto));
            verify(repositorio, never()).guardar(any());
            verify(eventPublisher, never()).publicar(any());
        }
    }

    @Nested
    @DisplayName("marcarNoRecibida()")
    class MarcarNoRecibida {

        @Test
        @DisplayName("Entrega EN_TRASLADO pasa a NO_RECIBIDA y publica ENTREGA_NO_RECIBIDA con el motivo")
        void marcaNoRecibida() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);

            NoRecibidaRequestDTO dto = new NoRecibidaRequestDTO();
            dto.setMotivo("No había nadie en la entidad");

            service.marcarNoRecibida(entrega.getId(), dto);

            assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstado());
            assertEquals("No había nadie en la entidad", entrega.getObservacion());

            ArgumentCaptor<EntregaEvento> captor = ArgumentCaptor.forClass(EntregaEvento.class);
            verify(eventPublisher).publicar(captor.capture());
            assertEquals(TipoEventoLogistica.ENTREGA_NO_RECIBIDA, captor.getValue().getTipo());
            assertEquals("No había nadie en la entidad", captor.getValue().getMotivo());
        }
    }

    @Nested
    @DisplayName("regresarADeposito()")
    class RegresarADeposito {

        @Test
        @DisplayName("Entrega NO_RECIBIDA vuelve a PENDIENTE sin disparar eventos")
        void regresaADeposito() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.NO_RECIBIDA);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);

            service.regresarADeposito(entrega.getId());

            assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado());
            verify(repositorio).guardar(entrega);
            verify(eventPublisher, never()).publicar(any());
        }
    }

    @Test
    @DisplayName("obtenerPorId() lanza EntregaNoEncontradaException si no existe")
    void obtenerPorIdInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(null);

        assertThrows(EntregaNoEncontradaException.class, () -> service.obtenerPorId(id));
    }

    @Test
    @DisplayName("obtenerPorEstado() delega en el repositorio")
    void obtenerPorEstadoDelegaEnRepositorio() {
        Entrega entrega = entregaEnEstado(EstadoEntrega.NO_RECIBIDA);
        when(repositorio.buscarPorEstado(EstadoEntrega.NO_RECIBIDA)).thenReturn(List.of(entrega));

        List<EntregaResponseDTO> resultado = service.obtenerPorEstado(EstadoEntrega.NO_RECIBIDA);

        assertEquals(1, resultado.size());
        assertEquals(entrega.getId(), resultado.getFirst().getId());
    }
}
