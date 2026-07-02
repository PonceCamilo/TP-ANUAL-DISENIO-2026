package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CallbackParadaDTO;
import ar.utn.donatrack.logistica.dtos.request.CallbackRutaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.CallbackVehiculoRutaDTO;
import ar.utn.donatrack.logistica.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.DonacionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.PlanificacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.exceptions.LoteCallbackInvalidoException;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.integracion.EntregaEventPublisher;
import ar.utn.donatrack.logistica.interfaces.integracion.EstrategiaRuteoPort;
import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.LotePlanificacionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import ar.utn.donatrack.logistica.models.planificacion.DonacionLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanificacionRutasServiceTest {

    @Mock
    private LotePlanificacionRepositoryInterface loteRepositorio;
    @Mock
    private RutaRepositoryInterface rutaRepositorio;
    @Mock
    private EntregaRepositoryInterface entregaRepositorio;
    @Mock
    private CamionRepositoryInterface camionRepositorio;
    @Mock
    private EstrategiaRuteoPort estrategiaRuteo;
    @Mock
    private EntregaEventPublisher eventPublisher;

    private PlanificacionRutasService service;

    @BeforeEach
    void setUp() {
        service = nuevoServicio(100);
    }

    private PlanificacionRutasService nuevoServicio(int maxDonacionesPorLote) {
        return new PlanificacionRutasService(
                loteRepositorio, rutaRepositorio, entregaRepositorio, camionRepositorio,
                estrategiaRuteo, eventPublisher, new EntregaValidator(), maxDonacionesPorLote);
    }

    private DireccionRequestDTO direccionDTO() {
        DireccionRequestDTO direccion = new DireccionRequestDTO();
        direccion.setCalle("Av. Siempre Viva");
        direccion.setNumero(742);
        direccion.setLocalidad("Springfield");
        direccion.setProvincia("Buenos Aires");
        direccion.setCodigoPostal("1900");
        return direccion;
    }

    private DonacionParaRutearRequestDTO donacionDTO(UUID idEntidad) {
        DonacionParaRutearRequestDTO donacion = new DonacionParaRutearRequestDTO();
        donacion.setIdDonacion(UUID.randomUUID());
        donacion.setIdEntidadBeneficiaria(idEntidad);
        donacion.setIdDonante(UUID.randomUUID());
        donacion.setDireccionEntrega(direccionDTO());
        return donacion;
    }

    @Nested
    @DisplayName("planificar()")
    class Planificar {

        @Test
        @DisplayName("Particiona las donaciones en lotes según el máximo configurado (Facade + Strategy)")
        void particionaEnLotesSegunElMaximo() {
            PlanificacionRutasService servicioConLotesChicos = nuevoServicio(2);

            UUID idEntidad = UUID.randomUUID();
            UUID idCamion = UUID.randomUUID();
            Camion camion = Camion.builder().id(idCamion).patente("AB123CD").build();
            when(camionRepositorio.buscarPorIds(List.of(idCamion))).thenReturn(List.of(camion));

            PlanificacionRequestDTO dto = new PlanificacionRequestDTO();
            dto.setCamionesIds(List.of(idCamion));
            dto.setDonaciones(List.of(donacionDTO(idEntidad), donacionDTO(idEntidad), donacionDTO(idEntidad)));

            List<LoteResponseDTO> lotes = servicioConLotesChicos.planificar(dto);

            assertEquals(2, lotes.size());
            verify(estrategiaRuteo, times(2)).solicitarPlanificacion(any(), any());
            verify(loteRepositorio, times(2)).guardar(any());

            ArgumentCaptor<LotePlanificacion> captor = ArgumentCaptor.forClass(LotePlanificacion.class);
            verify(loteRepositorio, times(2)).guardar(captor.capture());
            List<Integer> tamaniosDeLote = captor.getAllValues().stream().map(l -> l.getDonaciones().size()).toList();
            assertEquals(List.of(2, 1), tamaniosDeLote);
        }

        @Test
        @DisplayName("Con menos donaciones que el máximo, crea un único lote")
        void unaSolaDonacionCreaUnLote() {
            UUID idCamion = UUID.randomUUID();
            when(camionRepositorio.buscarPorIds(List.of(idCamion))).thenReturn(List.of());

            PlanificacionRequestDTO dto = new PlanificacionRequestDTO();
            dto.setCamionesIds(List.of(idCamion));
            dto.setDonaciones(List.of(donacionDTO(UUID.randomUUID())));

            List<LoteResponseDTO> lotes = service.planificar(dto);

            assertEquals(1, lotes.size());
            assertEquals(EstadoLote.ENVIADO, lotes.getFirst().getEstado());
            verify(estrategiaRuteo, times(1)).solicitarPlanificacion(any(), any());
        }
    }

    @Nested
    @DisplayName("registrarCallback()")
    class RegistrarCallback {

        @Test
        @DisplayName("Token de correlación inválido lanza LoteCallbackInvalidoException")
        void tokenInvalidoLanzaExcepcion() {
            UUID loteId = UUID.randomUUID();
            LotePlanificacion lote = LotePlanificacion.builder()
                    .id(loteId)
                    .tokenCorrelacion("token-correcto")
                    .donaciones(List.of())
                    .estado(EstadoLote.ENVIADO)
                    .build();
            when(loteRepositorio.buscarPorId(loteId)).thenReturn(lote);

            CallbackRutaRequestDTO dto = new CallbackRutaRequestDTO();
            dto.setLoteId(loteId);
            dto.setTokenCorrelacion("token-equivocado");
            dto.setRutas(List.of());

            assertThrows(LoteCallbackInvalidoException.class, () -> service.registrarCallback(dto));
        }

        @Test
        @DisplayName("Crea Ruta, Parada y Entrega a partir del callback, enriqueciendo con el donante del lote")
        void creaRutaYEntregasDesdeElCallback() {
            UUID loteId = UUID.randomUUID();
            UUID idDonacion = UUID.randomUUID();
            UUID idEntidad = UUID.randomUUID();
            UUID idDonante = UUID.randomUUID();
            UUID idCamion = UUID.randomUUID();

            LotePlanificacion lote = LotePlanificacion.builder()
                    .id(loteId)
                    .tokenCorrelacion("token-123")
                    .estado(EstadoLote.ENVIADO)
                    .donaciones(List.of(DonacionLote.builder()
                            .idDonacion(idDonacion)
                            .idEntidadBeneficiaria(idEntidad)
                            .idDonante(idDonante)
                            .build()))
                    .build();
            when(loteRepositorio.buscarPorId(loteId)).thenReturn(lote);

            Camion camion = Camion.builder().id(idCamion).build();
            when(camionRepositorio.buscarPorId(idCamion)).thenReturn(camion);

            CallbackParadaDTO parada = new CallbackParadaDTO();
            parada.setOrden(1);
            parada.setIdEntidadBeneficiaria(idEntidad);
            parada.setDireccion(direccionDTO());
            parada.setDonacionesIds(List.of(idDonacion));

            CallbackVehiculoRutaDTO vehiculo = new CallbackVehiculoRutaDTO();
            vehiculo.setCamionId(idCamion);
            vehiculo.setParadas(List.of(parada));

            CallbackRutaRequestDTO dto = new CallbackRutaRequestDTO();
            dto.setLoteId(loteId);
            dto.setTokenCorrelacion("token-123");
            dto.setRutas(List.of(vehiculo));

            service.registrarCallback(dto);

            ArgumentCaptor<Ruta> rutaCaptor = ArgumentCaptor.forClass(Ruta.class);
            verify(rutaRepositorio).guardar(rutaCaptor.capture());
            Ruta rutaGuardada = rutaCaptor.getValue();
            assertEquals(idCamion, rutaGuardada.getCamion().getId());
            assertEquals(EstadoRuta.PLANIFICADA, rutaGuardada.getEstado());
            assertEquals(1, rutaGuardada.getParadas().size());

            ArgumentCaptor<Entrega> entregaCaptor = ArgumentCaptor.forClass(Entrega.class);
            verify(entregaRepositorio).guardar(entregaCaptor.capture());
            Entrega entregaGuardada = entregaCaptor.getValue();
            assertEquals(idDonacion, entregaGuardada.getIdDonacion());
            assertEquals(idDonante, entregaGuardada.getIdDonante());
            assertEquals(EstadoEntrega.PENDIENTE, entregaGuardada.getEstado());

            ArgumentCaptor<LotePlanificacion> loteCaptor = ArgumentCaptor.forClass(LotePlanificacion.class);
            verify(loteRepositorio).guardar(loteCaptor.capture());
            assertEquals(EstadoLote.COMPLETADO, loteCaptor.getValue().getEstado());
            assertNotNull(loteCaptor.getValue().getFechaRespuesta());
        }
    }

    @Nested
    @DisplayName("iniciarRuta()")
    class IniciarRuta {

        @Test
        @DisplayName("Pone la ruta INICIADA, el camión EN_RUTA, las entregas EN_TRASLADO y publica INICIO_RUTA")
        void iniciaRutaYPropagaCambios() {
            UUID rutaId = UUID.randomUUID();
            UUID camionId = UUID.randomUUID();

            Camion camion = Camion.builder().id(camionId).estado(EstadoCamion.DISPONIBLE).build();

            Ruta ruta = Ruta.builder()
                    .id(rutaId)
                    .camion(camion)
                    .estado(EstadoRuta.PLANIFICADA)
                    .paradas(List.of())
                    .build();
            when(rutaRepositorio.buscarPorId(rutaId)).thenReturn(ruta);

            Entrega entrega = Entrega.builder()
                    .id(UUID.randomUUID())
                    .idDonacion(UUID.randomUUID())
                    .ruta(ruta)
                    .estado(EstadoEntrega.PENDIENTE)
                    .build();
            when(entregaRepositorio.buscarPorRutaId(rutaId)).thenReturn(List.of(entrega));

            service.iniciarRuta(rutaId);

            assertEquals(EstadoRuta.INICIADA, ruta.getEstado());
            assertNotNull(ruta.getFechaInicio());

            assertEquals(EstadoCamion.EN_RUTA, camion.getEstado());
            verify(camionRepositorio).guardar(camion);

            assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado());
            assertEquals(camionId, entrega.getCamion().getId());
            verify(entregaRepositorio).guardar(entrega);

            ArgumentCaptor<EntregaEvento> eventoCaptor = ArgumentCaptor.forClass(EntregaEvento.class);
            verify(eventPublisher).publicar(eventoCaptor.capture());
            assertEquals(TipoEventoLogistica.INICIO_RUTA, eventoCaptor.getValue().getTipo());
            assertEquals(rutaId, eventoCaptor.getValue().getRutaId());
        }
    }

    @Nested
    @DisplayName("obtenerRutaVigentePorCamion()")
    class ObtenerRutaVigente {

        @Test
        @DisplayName("Sin rutas activas para el camión, lanza RutaNoEncontradaException")
        void sinRutasActivasLanzaExcepcion() {
            UUID camionId = UUID.randomUUID();
            when(rutaRepositorio.buscarPorCamionId(camionId)).thenReturn(List.of());

            assertThrows(RutaNoEncontradaException.class, () -> service.obtenerRutaVigentePorCamion(camionId));
        }

        @Test
        @DisplayName("Ignora rutas FINALIZADA y devuelve la vigente")
        void devuelveLaRutaVigenteIgnorandoFinalizadas() {
            UUID camionId = UUID.randomUUID();
            Camion camion = Camion.builder().id(camionId).build();
            Ruta finalizada = Ruta.builder().id(UUID.randomUUID()).camion(camion)
                    .estado(EstadoRuta.FINALIZADA).paradas(List.of()).build();
            Ruta vigente = Ruta.builder().id(UUID.randomUUID()).camion(camion)
                    .estado(EstadoRuta.INICIADA).paradas(List.of()).build();
            when(rutaRepositorio.buscarPorCamionId(camionId)).thenReturn(List.of(finalizada, vigente));

            RutaResponseDTO resultado = service.obtenerRutaVigentePorCamion(camionId);

            assertEquals(vigente.getId(), resultado.getId());
        }
    }
}
