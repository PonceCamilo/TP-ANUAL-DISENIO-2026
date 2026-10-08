package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CallbackParadaDTO;
import ar.utn.donatrack.logistica.dtos.request.CallbackRutaRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.CallbackVehiculoRutaDTO;
import ar.utn.donatrack.logistica.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.DonacionParaRutearRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.PlanificacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.LoteResponseDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaPlanificadaProveedorDTO;
import ar.utn.donatrack.logistica.dtos.response.RutaResponseDTO;
import ar.utn.donatrack.logistica.eventos.EntregaEvento;
import ar.utn.donatrack.logistica.eventos.TipoEventoLogistica;
import ar.utn.donatrack.logistica.exceptions.CamionNoEncontradoException;
import ar.utn.donatrack.logistica.exceptions.LoteCallbackInvalidoException;
import ar.utn.donatrack.logistica.exceptions.LoteNoEncontradoException;
import ar.utn.donatrack.logistica.exceptions.ProveedorRuteoIndisponibleException;
import ar.utn.donatrack.logistica.exceptions.RutaNoEncontradaException;
import ar.utn.donatrack.logistica.exceptions.SinCamionesDisponiblesException;
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
import ar.utn.donatrack.logistica.models.planificacion.Parada;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import ar.utn.donatrack.logistica.validations.EntregaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests del orquestador de planificación de rutas.
 *
 * planificar() es un flujo de coordinación (particionar, repartir entre
 * camiones, llamar al proveedor, materializar Ruta/Parada/Entrega), así que
 * esos tests cubren el recorte del lote y el round-robin juntos. iniciarRuta()
 * sí se parte: un test el cambio de estados, otro la publicación del evento.
 *
 * El proveedor se mockea vía EstrategiaRuteoPort: acá no se prueba el HTTP
 * del adapter, solo que el service lo invoque y traduzca la respuesta.
 * El validador de transiciones de entrega se usa real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanificacionRutasService - lotes, callback, inicio y cierre de ruta")
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

    @Captor
    private ArgumentCaptor<LotePlanificacion> loteCaptor;
    @Captor
    private ArgumentCaptor<Entrega> entregaCaptor;
    @Captor
    private ArgumentCaptor<Ruta> rutaCaptor;
    @Captor
    private ArgumentCaptor<EntregaEvento> eventoCaptor;

    private PlanificacionRutasService servicio;

    @BeforeEach
    void prepararEscenario() {
        servicio = nuevoServicio(100);
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
        donacion.setDireccionEntrega(direccionDTO());
        return donacion;
    }

    /** Respuesta síncrona típica del proveedor: una sola parada agrupando lo recibido. */
    private RutaPlanificadaProveedorDTO rutaPlanificadaPara(UUID camionId, UUID idEntidad, List<UUID> idsDonaciones) {
        CallbackParadaDTO parada = new CallbackParadaDTO();
        parada.setOrden(1);
        parada.setIdEntidadBeneficiaria(idEntidad);
        parada.setDireccion(direccionDTO());
        parada.setDonacionesIds(idsDonaciones);

        RutaPlanificadaProveedorDTO ruta = new RutaPlanificadaProveedorDTO();
        ruta.setCamionId(camionId);
        ruta.setParadas(List.of(parada));
        return ruta;
    }

    private Camion camion(UUID id, String patente) {
        return Camion.builder().id(id).patente(patente).build();
    }

    @Nested
    @DisplayName("Planificación de lotes")
    class Planificacion {

        @Test
        @DisplayName("Particiona las donaciones en lotes según el máximo configurado y planifica cada lote contra el proveedor")
        void particionaEnLotesSegunElMaximo() {
            PlanificacionRutasService servicioConLotesChicos = nuevoServicio(2);

            UUID idEntidad = UUID.randomUUID();
            UUID idCamion = UUID.randomUUID();
            Camion camion = camion(idCamion, "AB123CD");
            when(camionRepositorio.buscarPorIds(List.of(idCamion))).thenReturn(List.of(camion));
            when(estrategiaRuteo.planificarParaCamion(any(), eq(camion), anyList()))
                    .thenAnswer(inv -> {
                        List<DonacionLote> donaciones = inv.getArgument(2);
                        List<UUID> ids = donaciones.stream().map(DonacionLote::getIdDonacion).toList();
                        return rutaPlanificadaPara(idCamion, idEntidad, ids);
                    });

            PlanificacionRequestDTO dto = new PlanificacionRequestDTO();
            dto.setCamionesIds(List.of(idCamion));
            dto.setDonaciones(List.of(donacionDTO(idEntidad), donacionDTO(idEntidad), donacionDTO(idEntidad)));

            List<LoteResponseDTO> lotes = servicioConLotesChicos.planificar(dto);

            assertThat(lotes).hasSize(2);
            assertThat(lotes.getFirst().getEstado()).isEqualTo(EstadoLote.COMPLETADO);
            verify(estrategiaRuteo, times(2)).planificarParaCamion(any(), eq(camion), anyList());
            verify(loteRepositorio, times(4)).guardar(loteCaptor.capture());
            List<Integer> tamaniosDeLote = loteCaptor.getAllValues().stream()
                    .map(l -> l.getDonaciones().size())
                    .distinct()
                    .sorted()
                    .toList();
            assertThat(tamaniosDeLote).containsExactly(1, 2);
        }

        @Test
        @DisplayName("Reparte las donaciones round-robin entre los camiones y arma una ruta por camión con destinos")
        void reparteDonacionesEntreCamiones() {
            UUID idEntidad1 = UUID.randomUUID();
            UUID idEntidad2 = UUID.randomUUID();
            UUID idCamion1 = UUID.randomUUID();
            UUID idCamion2 = UUID.randomUUID();
            Camion camion1 = camion(idCamion1, "AB123CD");
            Camion camion2 = camion(idCamion2, "XY987ZW");
            when(camionRepositorio.buscarPorIds(List.of(idCamion1, idCamion2))).thenReturn(List.of(camion1, camion2));

            when(estrategiaRuteo.planificarParaCamion(any(), eq(camion1), anyList()))
                    .thenAnswer(inv -> {
                        List<DonacionLote> donaciones = inv.getArgument(2);
                        return rutaPlanificadaPara(idCamion1, idEntidad1,
                                donaciones.stream().map(DonacionLote::getIdDonacion).toList());
                    });
            when(estrategiaRuteo.planificarParaCamion(any(), eq(camion2), anyList()))
                    .thenAnswer(inv -> {
                        List<DonacionLote> donaciones = inv.getArgument(2);
                        return rutaPlanificadaPara(idCamion2, idEntidad2,
                                donaciones.stream().map(DonacionLote::getIdDonacion).toList());
                    });

            PlanificacionRequestDTO dto = new PlanificacionRequestDTO();
            dto.setCamionesIds(List.of(idCamion1, idCamion2));
            dto.setDonaciones(List.of(donacionDTO(idEntidad1), donacionDTO(idEntidad2)));

            List<LoteResponseDTO> lotes = servicio.planificar(dto);

            assertThat(lotes).hasSize(1);
            LoteResponseDTO lote = lotes.getFirst();
            assertThat(lote.getEstado()).isEqualTo(EstadoLote.COMPLETADO);
            assertThat(lote.getFechaRespuesta()).isNotNull();
            assertThat(lote.getRutas()).hasSize(2);
            assertThat(lote.getRutas()).extracting(RutaResponseDTO::getCamionId)
                    .containsExactly(idCamion1, idCamion2);

            RutaResponseDTO rutaCamion1 = lote.getRutas().getFirst();
            assertThat(rutaCamion1.getParadas()).hasSize(1);
            assertThat(rutaCamion1.getParadas().getFirst().getIdEntidadBeneficiaria()).isEqualTo(idEntidad1);
            assertThat(rutaCamion1.getParadas().getFirst().getEntregasIds()).hasSize(1);

            verify(entregaRepositorio, times(2)).guardar(entregaCaptor.capture());
            assertThat(entregaCaptor.getAllValues())
                    .extracting(Entrega::getEstado)
                    .containsExactly(EstadoEntrega.LISTO_PARA_ENTREGAR, EstadoEntrega.LISTO_PARA_ENTREGAR);
        }

        @Test
        @DisplayName("Camión inexistente lanza 404 y no llama al proveedor")
        void camionInexistente() {
            UUID idCamion = UUID.randomUUID();
            when(camionRepositorio.buscarPorIds(List.of(idCamion))).thenReturn(List.of());

            PlanificacionRequestDTO dto = new PlanificacionRequestDTO();
            dto.setCamionesIds(List.of(idCamion));
            dto.setDonaciones(List.of(donacionDTO(UUID.randomUUID())));

            assertThatThrownBy(() -> servicio.planificar(dto))
                    .isInstanceOf(CamionNoEncontradoException.class);
            verify(estrategiaRuteo, never()).planificarParaCamion(any(), any(), any());
        }

        @Test
        @DisplayName("Si el proveedor no responde, se propaga ProveedorRuteoIndisponibleException")
        void proveedorIndisponible() {
            UUID idCamion = UUID.randomUUID();
            Camion camion = camion(idCamion, "AB123CD");
            when(camionRepositorio.buscarPorIds(List.of(idCamion))).thenReturn(List.of(camion));
            when(estrategiaRuteo.planificarParaCamion(any(), eq(camion), anyList()))
                    .thenThrow(new ProveedorRuteoIndisponibleException(idCamion, new RuntimeException("timeout")));

            PlanificacionRequestDTO dto = new PlanificacionRequestDTO();
            dto.setCamionesIds(List.of(idCamion));
            dto.setDonaciones(List.of(donacionDTO(UUID.randomUUID())));

            assertThatThrownBy(() -> servicio.planificar(dto))
                    .isInstanceOf(ProveedorRuteoIndisponibleException.class);
        }

        @Test
        @DisplayName("Sin camionesIds usa solo los camiones DISPONIBLE de la flota")
        void sinCamionesUsaLosDisponibles() {
            UUID idDisponible = UUID.randomUUID();
            UUID idEntidad = UUID.randomUUID();
            Camion disponible = camion(idDisponible, "AA111AA");
            Camion enRuta = camion(UUID.randomUUID(), "BB222BB");
            enRuta.setEstado(EstadoCamion.EN_RUTA);
            when(camionRepositorio.buscarTodos()).thenReturn(List.of(disponible, enRuta));

            DonacionParaRutearRequestDTO donacion = donacionDTO(idEntidad);
            when(estrategiaRuteo.planificarParaCamion(any(), eq(disponible), anyList()))
                    .thenReturn(rutaPlanificadaPara(idDisponible, idEntidad, List.of(donacion.getIdDonacion())));

            PlanificacionRequestDTO dto = new PlanificacionRequestDTO();
            dto.setDonaciones(List.of(donacion));

            List<LoteResponseDTO> lotes = servicio.planificar(dto);

            assertThat(lotes).hasSize(1);
            verify(estrategiaRuteo).planificarParaCamion(any(), eq(disponible), anyList());
            verify(estrategiaRuteo, never()).planificarParaCamion(any(), eq(enRuta), anyList());
            verify(camionRepositorio, never()).buscarPorIds(anyList());
        }

        @Test
        @DisplayName("Sin camionesIds y sin camiones DISPONIBLE lanza SinCamionesDisponiblesException")
        void sinCamionesDisponibles() {
            Camion enMantenimiento = camion(UUID.randomUUID(), "CC333CC");
            enMantenimiento.setEstado(EstadoCamion.MANTENIMIENTO);
            when(camionRepositorio.buscarTodos()).thenReturn(List.of(enMantenimiento));

            PlanificacionRequestDTO dto = new PlanificacionRequestDTO();
            dto.setCamionesIds(List.of());
            dto.setDonaciones(List.of(donacionDTO(UUID.randomUUID())));

            assertThatThrownBy(() -> servicio.planificar(dto))
                    .isInstanceOf(SinCamionesDisponiblesException.class);
            verifyNoInteractions(estrategiaRuteo);
        }
    }

    @Nested
    @DisplayName("Callback del proveedor de ruteo")
    class Callback {

        @Test
        @DisplayName("Token de correlación inválido lanza LoteCallbackInvalidoException")
        void tokenInvalido() {
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
            dto.setRutas(List.of());

            assertThatThrownBy(() -> servicio.registrarCallback(dto, "token-equivocado"))
                    .isInstanceOf(LoteCallbackInvalidoException.class);
        }

        @Test
        @DisplayName("Lanza 404 si el lote del callback no existe")
        void loteInexistente() {
            UUID loteId = UUID.randomUUID();
            when(loteRepositorio.buscarPorId(loteId)).thenReturn(null);

            CallbackRutaRequestDTO dto = new CallbackRutaRequestDTO();
            dto.setLoteId(loteId);
            dto.setRutas(List.of());

            assertThatThrownBy(() -> servicio.registrarCallback(dto, "token-123"))
                    .isInstanceOf(LoteNoEncontradoException.class);
        }

        @Test
        @DisplayName("Crea Ruta, Parada y Entrega a partir del callback; la entidad queda en la Parada")
        void creaRutaYEntregas() {
            UUID loteId = UUID.randomUUID();
            UUID idDonacion = UUID.randomUUID();
            UUID idEntidad = UUID.randomUUID();
            UUID idCamion = UUID.randomUUID();

            LotePlanificacion lote = LotePlanificacion.builder()
                    .id(loteId)
                    .tokenCorrelacion("token-123")
                    .estado(EstadoLote.ENVIADO)
                    .donaciones(List.of(DonacionLote.builder()
                            .idDonacion(idDonacion)
                            .idEntidadBeneficiaria(idEntidad)
                            .build()))
                    .build();
            when(loteRepositorio.buscarPorId(loteId)).thenReturn(lote);

            Camion camionDelCallback = Camion.builder().id(idCamion).build();
            when(camionRepositorio.buscarPorId(idCamion)).thenReturn(camionDelCallback);

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
            dto.setRutas(List.of(vehiculo));

            servicio.registrarCallback(dto, "token-123");

            verify(rutaRepositorio).guardar(rutaCaptor.capture());
            Ruta rutaGuardada = rutaCaptor.getValue();
            assertThat(rutaGuardada.getCamion().getId()).isEqualTo(idCamion);
            assertThat(rutaGuardada.getEstado()).isEqualTo(EstadoRuta.PLANIFICADA);
            assertThat(rutaGuardada.getParadas()).hasSize(1);
            assertThat(rutaGuardada.getParadas().getFirst().getIdEntidadBeneficiaria()).isEqualTo(idEntidad);

            verify(entregaRepositorio).guardar(entregaCaptor.capture());
            Entrega entregaGuardada = entregaCaptor.getValue();
            assertThat(entregaGuardada.getIdDonacion()).isEqualTo(idDonacion);
            assertThat(entregaGuardada.getEstado()).isEqualTo(EstadoEntrega.LISTO_PARA_ENTREGAR);
            assertThat(entregaGuardada.getParada()).isEqualTo(rutaGuardada.getParadas().getFirst());
            assertThat(rutaGuardada.obtenerEntregas()).hasSize(1);
            assertThat(rutaGuardada.obtenerEntregas().getFirst().getId()).isEqualTo(entregaGuardada.getId());

            verify(loteRepositorio).guardar(loteCaptor.capture());
            assertThat(loteCaptor.getValue().getEstado()).isEqualTo(EstadoLote.COMPLETADO);
            assertThat(loteCaptor.getValue().getFechaRespuesta()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Inicio de ruta")
    class InicioDeRuta {

        private Ruta rutaPlanificadaConEntrega(UUID rutaId, Camion camion, Entrega entrega) {
            Parada parada = Parada.builder()
                    .id(UUID.randomUUID())
                    .orden(1)
                    .entregas(new ArrayList<>())
                    .build();
            entrega.setParada(parada);
            parada.getEntregas().add(entrega);
            return Ruta.builder()
                    .id(rutaId)
                    .camion(camion)
                    .estado(EstadoRuta.PLANIFICADA)
                    .paradas(List.of(parada))
                    .build();
        }

        @Test
        @DisplayName("Pone la ruta INICIADA, el camión EN_RUTA y las entregas EN_TRASLADO")
        void pasaAEnTraslado() {
            UUID rutaId = UUID.randomUUID();
            Camion camionDelViaje = Camion.builder()
                    .id(UUID.randomUUID())
                    .estado(EstadoCamion.DISPONIBLE)
                    .build();
            Entrega entrega = Entrega.builder()
                    .id(UUID.randomUUID())
                    .idDonacion(UUID.randomUUID())
                    .estado(EstadoEntrega.LISTO_PARA_ENTREGAR)
                    .build();
            Ruta ruta = rutaPlanificadaConEntrega(rutaId, camionDelViaje, entrega);
            when(rutaRepositorio.buscarPorId(rutaId)).thenReturn(ruta);

            servicio.iniciarRuta(rutaId);

            assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.INICIADA);
            assertThat(ruta.getFechaInicio()).isNotNull();
            assertThat(camionDelViaje.getEstado()).isEqualTo(EstadoCamion.EN_RUTA);
            verify(camionRepositorio).guardar(camionDelViaje);
            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.EN_TRASLADO);
            verify(entregaRepositorio).guardar(entrega);
        }

        @Test
        @DisplayName("Publica un solo INICIO_RUTA con las donaciones de la ruta y el link al mapa")
        void publicaInicioRuta() {
            UUID rutaId = UUID.randomUUID();
            UUID idDonacion = UUID.randomUUID();
            Camion camionDelViaje = Camion.builder()
                    .id(UUID.randomUUID())
                    .estado(EstadoCamion.DISPONIBLE)
                    .build();
            Entrega entrega = Entrega.builder()
                    .id(UUID.randomUUID())
                    .idDonacion(idDonacion)
                    .estado(EstadoEntrega.LISTO_PARA_ENTREGAR)
                    .build();
            Ruta ruta = rutaPlanificadaConEntrega(rutaId, camionDelViaje, entrega);
            when(rutaRepositorio.buscarPorId(rutaId)).thenReturn(ruta);

            servicio.iniciarRuta(rutaId);

            verify(eventPublisher).publicar(eventoCaptor.capture());
            EntregaEvento evento = eventoCaptor.getValue();
            assertThat(evento.getTipo()).isEqualTo(TipoEventoLogistica.INICIO_RUTA);
            assertThat(evento.getRutaId()).isEqualTo(rutaId);
            assertThat(evento.getIdsDonaciones()).containsExactly(idDonacion);
            assertThat(evento.getUrlMapaInteractivo()).contains(rutaId.toString());
        }

        @Test
        @DisplayName("Una ruta sin entregas se inicia igual pero no publica evento")
        void sinEntregasNoPublica() {
            UUID rutaId = UUID.randomUUID();
            Ruta ruta = Ruta.builder()
                    .id(rutaId)
                    .estado(EstadoRuta.PLANIFICADA)
                    .paradas(List.of())
                    .build();
            when(rutaRepositorio.buscarPorId(rutaId)).thenReturn(ruta);

            servicio.iniciarRuta(rutaId);

            assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.INICIADA);
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("Cierre de ruta")
    class CierreDeRuta {

        @Test
        @DisplayName("Con entregas aún EN_TRASLADO, no finaliza la ruta ni libera el camión")
        void noFinalizaSiQuedanEntregasEnTraslado() {
            UUID rutaId = UUID.randomUUID();
            Camion camionEnRuta = Camion.builder().id(UUID.randomUUID()).estado(EstadoCamion.EN_RUTA).build();
            Parada parada = Parada.builder()
                    .id(UUID.randomUUID())
                    .orden(1)
                    .entregas(List.of(
                            Entrega.builder().id(UUID.randomUUID()).estado(EstadoEntrega.ENTREGADA).build(),
                            Entrega.builder().id(UUID.randomUUID()).estado(EstadoEntrega.EN_TRASLADO).build()))
                    .build();
            Ruta ruta = Ruta.builder()
                    .id(rutaId)
                    .camion(camionEnRuta)
                    .estado(EstadoRuta.INICIADA)
                    .paradas(List.of(parada))
                    .build();
            when(rutaRepositorio.buscarPorId(rutaId)).thenReturn(ruta);

            servicio.finalizarRutaSiCorresponde(rutaId);

            assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.INICIADA);
            assertThat(camionEnRuta.getEstado()).isEqualTo(EstadoCamion.EN_RUTA);
            verify(rutaRepositorio, never()).guardar(any());
            verify(camionRepositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Con todas las entregas en estado terminal, finaliza la ruta y libera el camión")
        void finalizaYLiberaCamion() {
            UUID rutaId = UUID.randomUUID();
            Camion camionEnRuta = Camion.builder().id(UUID.randomUUID()).estado(EstadoCamion.EN_RUTA).build();
            Parada parada = Parada.builder()
                    .id(UUID.randomUUID())
                    .orden(1)
                    .entregas(List.of(
                            Entrega.builder().id(UUID.randomUUID()).estado(EstadoEntrega.ENTREGADA).build(),
                            Entrega.builder().id(UUID.randomUUID()).estado(EstadoEntrega.NO_RECIBIDA).build()))
                    .build();
            Ruta ruta = Ruta.builder()
                    .id(rutaId)
                    .camion(camionEnRuta)
                    .estado(EstadoRuta.INICIADA)
                    .paradas(List.of(parada))
                    .build();
            when(rutaRepositorio.buscarPorId(rutaId)).thenReturn(ruta);

            servicio.finalizarRutaSiCorresponde(rutaId);

            assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.FINALIZADA);
            verify(rutaRepositorio).guardar(ruta);
            assertThat(camionEnRuta.getEstado()).isEqualTo(EstadoCamion.DISPONIBLE);
            verify(camionRepositorio).guardar(camionEnRuta);
        }

        @Test
        @DisplayName("Una ruta que no está INICIADA se ignora")
        void ignoraRutasQueNoEstanIniciadas() {
            UUID rutaId = UUID.randomUUID();
            Ruta ruta = Ruta.builder().id(rutaId).estado(EstadoRuta.PLANIFICADA).paradas(List.of()).build();
            when(rutaRepositorio.buscarPorId(rutaId)).thenReturn(ruta);

            servicio.finalizarRutaSiCorresponde(rutaId);

            assertThat(ruta.getEstado()).isEqualTo(EstadoRuta.PLANIFICADA);
            verify(rutaRepositorio, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("Ruta vigente de un camión")
    class RutaVigente {

        @Test
        @DisplayName("Sin rutas activas para el camión, lanza RutaNoEncontradaException")
        void sinRutasActivas() {
            UUID camionId = UUID.randomUUID();
            when(rutaRepositorio.buscarPorCamionId(camionId)).thenReturn(List.of());

            assertThatThrownBy(() -> servicio.obtenerRutaVigentePorCamion(camionId))
                    .isInstanceOf(RutaNoEncontradaException.class);
        }

        @Test
        @DisplayName("Ignora rutas FINALIZADA y devuelve la vigente")
        void ignoraFinalizadas() {
            UUID camionId = UUID.randomUUID();
            Camion camionDelPedido = Camion.builder().id(camionId).build();
            Ruta finalizada = Ruta.builder().id(UUID.randomUUID()).camion(camionDelPedido)
                    .estado(EstadoRuta.FINALIZADA).paradas(List.of()).build();
            Ruta vigente = Ruta.builder().id(UUID.randomUUID()).camion(camionDelPedido)
                    .estado(EstadoRuta.INICIADA).paradas(List.of()).build();
            when(rutaRepositorio.buscarPorCamionId(camionId)).thenReturn(List.of(finalizada, vigente));

            RutaResponseDTO resultado = servicio.obtenerRutaVigentePorCamion(camionId);

            assertThat(resultado.getId()).isEqualTo(vigente.getId());
        }
    }
}
