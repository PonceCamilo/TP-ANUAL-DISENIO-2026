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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests de las operaciones sobre una entrega ya creada por la planificación.
 *
 * Cubre consulta, confirmación de recepción, marca de no recibida y regreso
 * a depósito. Por cada cambio de estado se verifica por separado: (1) que el
 * modelo quede bien, (2) que se publique el evento hacia el broker. La lógica de
 * qué transiciones son válidas está en EntregaValidatorTest; acá se comprueba
 * que el service la respete y no dispare efectos colaterales si falla.
 *
 * Se usa el validador real y se mockean repositorios, publisher y el service
 * de planificación (para no re-probar finalizarRutaSiCorresponde acá).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntregaService - operaciones sobre entregas")
class EntregaServiceTest {

    private static final String PATENTE = "AB123CD";

    @Mock
    private EntregaRepositoryInterface repositorio;

    @Mock
    private RutaRepositoryInterface rutaRepositorio;

    @Mock
    private EntregaEventPublisher eventPublisher;

    @Mock
    private PlanificacionServiceInterface planificacionService;

    @Captor
    private ArgumentCaptor<EntregaEvento> eventoCaptor;

    private EntregaService servicio;

    @BeforeEach
    void prepararEscenario() {
        servicio = new EntregaService(
                repositorio, rutaRepositorio, new EntregaValidator(), eventPublisher, planificacionService);
    }

    private Entrega entregaEnEstado(EstadoEntrega estado) {
        return Entrega.builder()
                .id(UUID.randomUUID())
                .idDonacion(UUID.randomUUID())
                .estado(estado)
                .build();
    }

    private Camion camion() {
        return Camion.builder().id(UUID.randomUUID()).patente(PATENTE).build();
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

    private ConfirmarEntregaRequestDTO dtoConfirmar() {
        ConfirmarEntregaRequestDTO dto = new ConfirmarEntregaRequestDTO();
        dto.setFotosComprobante(List.of("foto1.jpg"));
        return dto;
    }

    private NoRecibidaRequestDTO dtoNoRecibida(MotivoFalloEntrega motivo) {
        NoRecibidaRequestDTO dto = new NoRecibidaRequestDTO();
        dto.setMotivo(motivo);
        return dto;
    }

    @Nested
    @DisplayName("Consulta de entregas")
    class Consulta {

        @Test
        @DisplayName("obtenerPorId() devuelve la entrega con ruta y camión resueltos por query inversa")
        void obtenerPorId() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Camion camion = camion();
            Ruta ruta = rutaQueContiene(entrega, camion);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            EntregaResponseDTO dto = servicio.obtenerPorId(entrega.getId());

            assertThat(dto.getId()).isEqualTo(entrega.getId());
            assertThat(dto.getEstado()).isEqualTo(EstadoEntrega.EN_TRASLADO);
            assertThat(dto.getRutaId()).isEqualTo(ruta.getId());
            assertThat(dto.getCamionId()).isEqualTo(camion.getId());
        }

        @Test
        @DisplayName("obtenerPorId() lanza 404 si la entrega no existe")
        void obtenerPorIdInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.obtenerPorId(idInexistente))
                    .isInstanceOf(EntregaNoEncontradaException.class);
        }

        @Test
        @DisplayName("obtenerPorId() no rompe si la entrega todavía no está en ninguna ruta")
        void obtenerPorIdSinRuta() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.LISTO_PARA_ENTREGAR);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.empty());

            EntregaResponseDTO dto = servicio.obtenerPorId(entrega.getId());

            assertThat(dto.getId()).isEqualTo(entrega.getId());
            assertThat(dto.getRutaId()).isNull();
            assertThat(dto.getCamionId()).isNull();
        }

        @Test
        @DisplayName("obtenerPorEstado() delega en el repositorio y completa rutaId y camionId")
        void obtenerPorEstado() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.NO_RECIBIDA);
            Camion camion = camion();
            Ruta ruta = rutaQueContiene(entrega, camion);
            when(repositorio.buscarPorEstado(EstadoEntrega.NO_RECIBIDA)).thenReturn(List.of(entrega));
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            List<EntregaResponseDTO> resultado = servicio.obtenerPorEstado(EstadoEntrega.NO_RECIBIDA);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().getId()).isEqualTo(entrega.getId());
            assertThat(resultado.getFirst().getRutaId()).isEqualTo(ruta.getId());
            assertThat(resultado.getFirst().getCamionId()).isEqualTo(camion.getId());
        }
    }

    @Nested
    @DisplayName("Confirmación de recepción")
    class Confirmacion {

        @Test
        @DisplayName("Pasa la entrega a ENTREGADA, guarda las fotos y registra el cambio en el historial")
        void pasaAEntregada() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Ruta ruta = rutaQueContiene(entrega, camion());
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            servicio.confirmar(entrega.getId(), dtoConfirmar());

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ENTREGADA);
            assertThat(entrega.getFotosComprobante()).contains("foto1.jpg");
            assertThat(entrega.getFechaEntrega()).isNotNull();
            assertThat(entrega.getHistorial()).hasSize(1);
            assertThat(entrega.getHistorial().getFirst().getEstado()).isEqualTo(EstadoEntrega.ENTREGADA);
            verify(repositorio).guardar(entrega);
        }

        @Test
        @DisplayName("Publica ENTREGA_CONFIRMADA con patente, ruta, camión y entidad de la parada")
        void publicaEntregaConfirmada() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Camion camion = camion();
            Ruta ruta = rutaQueContiene(entrega, camion);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            servicio.confirmar(entrega.getId(), dtoConfirmar());

            verify(eventPublisher).publicar(eventoCaptor.capture());
            EntregaEvento evento = eventoCaptor.getValue();
            assertThat(evento.getTipo()).isEqualTo(TipoEventoLogistica.ENTREGA_CONFIRMADA);
            assertThat(evento.getRutaId()).isEqualTo(ruta.getId());
            assertThat(evento.getIdCamion()).isEqualTo(camion.getId());
            assertThat(evento.getPatenteCamion()).isEqualTo(PATENTE);
            assertThat(evento.getIdEntidadBeneficiaria()).isEqualTo(entrega.getParada().getIdEntidadBeneficiaria());
            assertThat(evento.getIdDonacion()).isEqualTo(entrega.getIdDonacion());
        }

        @Test
        @DisplayName("Después de confirmar consulta si la ruta debe finalizarse")
        void consultaFinalizacionDeRuta() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Ruta ruta = rutaQueContiene(entrega, camion());
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            servicio.confirmar(entrega.getId(), dtoConfirmar());

            verify(planificacionService).finalizarRutaSiCorresponde(ruta.getId());
        }

        @Test
        @DisplayName("Si no hay ruta para la entrega, lanza 404 y no consulta finalización")
        void sinRutaNoConsultaFinalizacion() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.confirmar(entrega.getId(), dtoConfirmar()))
                    .isInstanceOf(RutaNoEncontradaException.class);
            verify(planificacionService, never()).finalizarRutaSiCorresponde(any());
        }

        @Test
        @DisplayName("Una entrega LISTO_PARA_ENTREGAR no puede confirmarse: no se guarda ni se publica")
        void estadoIncompatible() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.LISTO_PARA_ENTREGAR);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);

            assertThatThrownBy(() -> servicio.confirmar(entrega.getId(), dtoConfirmar()))
                    .isInstanceOf(TransicionEntregaIlegalException.class);
            verify(repositorio, never()).guardar(any());
            verifyNoInteractions(eventPublisher);
            verify(rutaRepositorio, never()).buscarPorEntregaId(any());
            verifyNoInteractions(planificacionService);
        }

        @Test
        @DisplayName("Lanza 404 si la entrega a confirmar no existe")
        void entregaInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.confirmar(idInexistente, dtoConfirmar()))
                    .isInstanceOf(EntregaNoEncontradaException.class);
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("Entrega no recibida")
    class NoRecibida {

        @Test
        @DisplayName("Pasa la entrega a NO_RECIBIDA y guarda el motivo como observación")
        void pasaANoRecibida() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Ruta ruta = rutaQueContiene(entrega, camion());
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            servicio.marcarNoRecibida(entrega.getId(), dtoNoRecibida(MotivoFalloEntrega.ENTIDAD_AUSENTE));

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.NO_RECIBIDA);
            assertThat(entrega.getObservacion()).isEqualTo("ENTIDAD_AUSENTE");
            verify(repositorio).guardar(entrega);
        }

        @Test
        @DisplayName("Publica ENTREGA_NO_RECIBIDA con motivo, ruta, camión y replanificable=true")
        void publicaEntregaNoRecibida() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Camion camion = camion();
            Ruta ruta = rutaQueContiene(entrega, camion);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            servicio.marcarNoRecibida(entrega.getId(), dtoNoRecibida(MotivoFalloEntrega.ENTIDAD_AUSENTE));

            verify(eventPublisher).publicar(eventoCaptor.capture());
            EntregaEvento evento = eventoCaptor.getValue();
            assertThat(evento.getTipo()).isEqualTo(TipoEventoLogistica.ENTREGA_NO_RECIBIDA);
            assertThat(evento.getMotivoFallo()).isEqualTo("ENTIDAD_AUSENTE");
            assertThat(evento.getReplanificable()).isTrue();
            assertThat(evento.getRutaId()).isEqualTo(ruta.getId());
            assertThat(evento.getIdCamion()).isEqualTo(camion.getId());
        }

        @Test
        @DisplayName("Un motivo de mercadería rota se publica como no replanificable")
        void motivoNoReplanificable() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Ruta ruta = rutaQueContiene(entrega, camion());
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            servicio.marcarNoRecibida(entrega.getId(), dtoNoRecibida(MotivoFalloEntrega.MERCADERIA_ROTA));

            verify(eventPublisher).publicar(eventoCaptor.capture());
            assertThat(eventoCaptor.getValue().getReplanificable()).isFalse();
            assertThat(eventoCaptor.getValue().getMotivoFallo()).isEqualTo("MERCADERIA_ROTA");
        }

        @Test
        @DisplayName("Después de marcar no recibida consulta si la ruta debe finalizarse")
        void consultaFinalizacionDeRuta() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.EN_TRASLADO);
            Ruta ruta = rutaQueContiene(entrega, camion());
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);
            when(rutaRepositorio.buscarPorEntregaId(entrega.getId())).thenReturn(Optional.of(ruta));

            servicio.marcarNoRecibida(entrega.getId(), dtoNoRecibida(MotivoFalloEntrega.ENTIDAD_AUSENTE));

            verify(planificacionService).finalizarRutaSiCorresponde(ruta.getId());
        }

        @Test
        @DisplayName("Una entrega que no está EN_TRASLADO no puede marcarse como no recibida")
        void estadoIncompatible() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.LISTO_PARA_ENTREGAR);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);

            assertThatThrownBy(() -> servicio.marcarNoRecibida(
                    entrega.getId(), dtoNoRecibida(MotivoFalloEntrega.ENTIDAD_AUSENTE)))
                    .isInstanceOf(TransicionEntregaIlegalException.class);
            verify(repositorio, never()).guardar(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Lanza 404 si la entrega no existe")
        void entregaInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.marcarNoRecibida(
                    idInexistente, dtoNoRecibida(MotivoFalloEntrega.ENTIDAD_AUSENTE)))
                    .isInstanceOf(EntregaNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Regreso a depósito")
    class RegresoADeposito {

        @Test
        @DisplayName("Una entrega NO_RECIBIDA vuelve a LISTO_PARA_ENTREGAR sin publicar eventos")
        void vuelveAListoParaEntregar() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.NO_RECIBIDA);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);

            servicio.regresarADeposito(entrega.getId());

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.LISTO_PARA_ENTREGAR);
            assertThat(entrega.getObservacion()).isEqualTo("Regreso a depósito");
            verify(repositorio).guardar(entrega);
            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(planificacionService);
        }

        @Test
        @DisplayName("Una entrega ENTREGADA no puede volver al depósito")
        void estadoIncompatible() {
            Entrega entrega = entregaEnEstado(EstadoEntrega.ENTREGADA);
            when(repositorio.buscarPorId(entrega.getId())).thenReturn(entrega);

            assertThatThrownBy(() -> servicio.regresarADeposito(entrega.getId()))
                    .isInstanceOf(TransicionEntregaIlegalException.class);
            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Lanza 404 si la entrega no existe")
        void entregaInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.regresarADeposito(idInexistente))
                    .isInstanceOf(EntregaNoEncontradaException.class);
        }
    }
}
