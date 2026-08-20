package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.ConfirmarEntregaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.NoRecibidaRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.EntregaResponseDTO;
import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.exceptions.EntregaNoEncontradaException;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.exceptions.TransicionEntregaIlegalException;
import ar.utn.donatrack.logistica.integracion.EntregaEventPublisher;
import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.services.PlanificacionServiceInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.entrega.MotivoFalloEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.Parada;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
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
import java.util.Optional;
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
    private RutaRepositoryInterface rutaRepositorio;

    @Mock
    private EntregaEventPublisher eventPublisher;

    @Mock
    private PlanificacionServiceInterface planificacionService;

    private EntregaService service;

    @BeforeEach
    void setUp() {
        service = new EntregaService(repositorio, rutaRepositorio, new EntregaValidator(), eventPublisher, planificacionService);
    }

    private Entrega entregaEnEstado(EstadoEntrega estado) {
        return Entrega.builder()
                .id(UUID.randomUUID())
                .idDonacion(UUID.randomUUID())
                .estado(estado)
                .build();
    }

    private Ruta rutaQueContiene(Entrega entrega, Camion camion) {
        Parada parada = Parada.builder()
                .id(UUID.randomUUID())
                .orden(1)
                .idEntidadBeneficiaria(UUID.randomUUID())
                .entregas(List.of(entrega))
                .build();
        entrega.setParada(parada);
        return Ruta.builder()
                .id(UUID.randomUUID())
                .camion(camion)
                .paradas(List.of(parada))
                .build();
    }

    @Nested
    @DisplayName("confirmar()")
    class Confirmar {

        @Test
        @DisplayName("Entrega EN_TRASLADO pasa a ENTREGADA, guarda las fotos y publica ENTREGA_CONFIRMADA con datos de la Ruta")
        void confirmaEntregaEnTraslado() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Camion camion = Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build();
            Ruta ruta = rutaQueContiene(entrega, camion);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO();
            dto.setFotosComprobante(List.of("foto1.jpg"));

            service.confirmar(entrega.getId(), dto);

            assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstado());
            assertTrue(entrega.getFotosComprobante().contains("foto1.jpg"));
            verify(repositorio).guardar(entrega);

            ArgumentCaptor<EntregaEvento> captor = ArgumentCaptor.forClass(EntregaEvento.class);
            verify(eventPublisher).publicar(captor.capture());
            EntregaEvento evento = captor.getValue();
            assertEquals(TipoEventoLogistica.ENTREGA_CONFIRMADA, evento.getTipo());
            assertEquals(ruta.getId(), evento.getRutaId());
            assertEquals(camion.getId(), evento.getIdCamion());
            assertEquals("AB123CD", evento.getPatenteCamion());
            assertEquals(entrega.getParada().getIdEntidadBeneficiaria(), evento.getIdEntidadBeneficiaria());
            verify(planificacionService).finalizarRutaSiCorresponde(ruta.getId());
        }

        @Test
        @DisplayName("Consulta si la ruta debe finalizarse usando la query inversa")
        void consultaFinalizacionDeRutaCuandoHayRutaAsignada() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Camion camion = Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build();
            Ruta ruta = rutaQueContiene(entrega, camion);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO();
            dto.setFotosComprobante(List.of("foto1.jpg"));

            service.confirmar(entrega.getId(), dto);

            verify(planificacionService).finalizarRutaSiCorresponde(ruta.getId());
        }

        @Test
        @DisplayName("Si no hay ruta para la entrega, lanza RutaNoEncontradaException y no consulta finalización")
        void noConsultaFinalizacionDeRutaSinRutaAsignada() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.empty());

            ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO();
            dto.setFotosComprobante(List.of("foto1.jpg"));

            assertThrows(RutaNoEncontradaException.class, () -> service.confirmar(entrega.getId(), dto));
            verify(planificacionService, never()).finalizarRutaSiCorresponde(any());
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
            verify(rutaRepositorio, never()).buscarPorEntregaId(any());
            verify(planificacionService, never()).finalizarRutaSiCorresponde(any());
        }
    }

    @Nested
    @DisplayName("marcarNoRecibida()")
    class MarcarNoRecibida {

        @Test
        @DisplayName("Entrega EN_TRASLADO pasa a NO_RECIBIDA y publica ENTREGA_NO_RECIBIDA con ruta y camión")
        void marcaNoRecibida() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Camion camion = Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build();
            Ruta ruta = rutaQueContiene(entrega, camion);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            NoRecibidaRequestDTO dto = new NoRecibidaRequestDTO();
            dto.setMotivo(MotivoFalloEntrega.ENTIDAD_AUSENTE);

            service.marcarNoRecibida(entrega.getId(), dto);

            assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstado());
            assertEquals("ENTIDAD_AUSENTE", entrega.getObservacion());

            ArgumentCaptor<EntregaEvento> captor = ArgumentCaptor.forClass(EntregaEvento.class);
            verify(eventPublisher).publicar(captor.capture());
            EntregaEvento evento = captor.getValue();
            assertEquals(TipoEventoLogistica.ENTREGA_NO_RECIBIDA, evento.getTipo());
            assertEquals("ENTIDAD_AUSENTE", evento.getMotivoFallo());
            assertTrue(evento.getReplanificable());
            assertEquals(ruta.getId(), evento.getRutaId());
            assertEquals(camion.getId(), evento.getIdCamion());
            verify(planificacionService).finalizarRutaSiCorresponde(ruta.getId());
        }

        @Test
        @DisplayName("Consulta si la ruta debe finalizarse usando la query inversa")
        void consultaFinalizacionDeRutaCuandoHayRutaAsignada() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Camion camion = Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build();
            Ruta ruta = rutaQueContiene(entrega, camion);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            NoRecibidaRequestDTO dto = new NoRecibidaRequestDTO();
            dto.setMotivo(MotivoFalloEntrega.MERCADERIA_ROTA);

            service.marcarNoRecibida(entrega.getId(), dto);

            verify(planificacionService).finalizarRutaSiCorresponde(ruta.getId());
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
            verify(planificacionService, never()).finalizarRutaSiCorresponde(any());
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
    @DisplayName("obtenerPorEstado() delega en el repositorio y resuelve rutaId/camionId por query inversa")
    void obtenerPorEstadoDelegaEnRepositorio() {
        Entrega entrega = entregaEnEstado(EstadoEntrega.NO_RECIBIDA);
        Camion camion = Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build();
        Ruta ruta = rutaQueContiene(entrega, camion);
        when(repositorio.buscarPorEstado(EstadoEntrega.NO_RECIBIDA)).thenReturn(List.of(entrega));
        when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

        List<EntregaResponseDTO> resultado = service.obtenerPorEstado(EstadoEntrega.NO_RECIBIDA);

        assertEquals(1, resultado.size());
        assertEquals(entrega.getId(), resultado.getFirst().getId());
        assertEquals(ruta.getId(), resultado.getFirst().getRutaId());
        assertEquals(camion.getId(), resultado.getFirst().getCamionId());
    }
}
