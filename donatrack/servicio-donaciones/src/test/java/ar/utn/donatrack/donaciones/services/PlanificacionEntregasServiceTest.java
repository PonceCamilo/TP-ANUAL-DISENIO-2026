package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.integracion.logistica.BrokerLogistica;
import ar.utn.donatrack.donaciones.integracion.logistica.ResultadoPlanificacion;
import ar.utn.donatrack.donaciones.integracion.logistica.SolicitudEntrega;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.Localidad;
import ar.utn.donatrack.donaciones.models.entidad.Provincia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests del proceso que dispara la planificación de entregas.
 *
 * Cierra la cadena que hasta ahora estaba cortada: nadie invocaba la
 * planificación de rutas, así que el endpoint de logística existía sin uso.
 *
 *   matchmaking (03:30) ─▶ el administrador confirma ─▶ este proceso (04:00) ─▶ broker
 *
 * Verifica tres cosas: a quién se deriva, qué se le manda, y qué pasa con el
 * estado de las donaciones según el proveedor acepte o no.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanificacionEntregasService - derivación de las entregas del día siguiente")
class PlanificacionEntregasServiceTest {

    @Mock
    private DonacionesRepositoryInterface donacionesRepositorio;

    @Mock
    private EntidadesBeneficiariasRepositoryInterface entidadesRepositorio;

    @Mock
    private BrokerLogistica broker;

    @InjectMocks
    private PlanificacionEntregasService servicio;

    @Captor
    private ArgumentCaptor<List<SolicitudEntrega>> entregasCaptor;

    private EntidadBeneficiaria entidad;

    @BeforeEach
    void prepararEntidad() {
        entidad = EntidadBeneficiaria.builder()
                .id(UUID.randomUUID())
                .razonSocial("Comedor Los Pibes")
                .direccion(Direccion.builder()
                        .calle("Medrano")
                        .numero(951)
                        .codigoPostal("C1179")
                        .localidad(Localidad.builder()
                                .nombre("CABA")
                                .provincia(Provincia.builder().nombre("Buenos Aires").build())
                                .build())
                        .build())
                .campanias(new ArrayList<>())
                .build();
    }

    /** Donación ya asignada a la entidad del escenario (estado ASIGNACION_REALIZADA). */
    private Donacion donacionAsignada() {
        Donacion donacion = new Donacion();
        donacion.setSubcategoria(new Subcategoria("arroz"));
        donacion.setIdDonante(UUID.randomUUID());
        donacion.asignarA(entidad);
        return donacion;
    }

    @Nested
    @DisplayName("Armado de las solicitudes")
    class ArmadoDeSolicitudes {

        @Test
        @DisplayName("Solo toma las donaciones en ASIGNACION_REALIZADA")
        void soloLasAsignadas() {
            // Una donación todavía en depósito no tiene destino: no se puede rutear.
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).thenReturn(List.of());

            servicio.planificarEntregasDelDiaSiguiente();

            verify(donacionesRepositorio).obtenerPorEstado("ASIGNACION_REALIZADA");
            verifyNoInteractions(broker);
        }

        @Test
        @DisplayName("Envía una solicitud por donación, con la dirección de la entidad destino")
        void unaSolicitudPorDonacion() {
            Donacion primera = donacionAsignada();
            Donacion segunda = donacionAsignada();
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA"))
                    .thenReturn(List.of(primera, segunda));
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(broker.planificar(anyList()))
                    .thenReturn(ResultadoPlanificacion.aceptada("propia", List.of()));

            servicio.planificarEntregasDelDiaSiguiente();

            verify(broker).planificar(entregasCaptor.capture());
            List<SolicitudEntrega> enviadas = entregasCaptor.getValue();

            assertThat(enviadas).hasSize(2);
            assertThat(enviadas).extracting(SolicitudEntrega::idDonacion)
                    .containsExactlyInAnyOrder(primera.getId(), segunda.getId());
            assertThat(enviadas.getFirst().direccionEntrega().getCalle()).isEqualTo("Medrano");
        }

        @Test
        @DisplayName("Saltea las donaciones cuya entidad destino no existe")
        void entidadInexistenteSeSaltea() {
            // Caso borde: la entidad se eliminó después de la asignación. Frenar
            // todo el lote por una sola sería peor que dejarla afuera.
            Donacion donacion = donacionAsignada();
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).thenReturn(List.of(donacion));
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(null);

            servicio.planificarEntregasDelDiaSiguiente();

            verify(broker, never()).planificar(anyList());
        }

        @Test
        @DisplayName("Saltea las donaciones cuya entidad no tiene dirección cargada")
        void entidadSinDireccionSeSaltea() {
            // Sin dirección no hay ruta posible.
            EntidadBeneficiaria sinDireccion = EntidadBeneficiaria.builder()
                    .id(entidad.getId())
                    .razonSocial("Comedor sin dirección")
                    .build();
            Donacion donacion = donacionAsignada();
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).thenReturn(List.of(donacion));
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(sinDireccion);

            servicio.planificarEntregasDelDiaSiguiente();

            verify(broker, never()).planificar(anyList());
        }

        @Test
        @DisplayName("Una donación sin entidad no frena a las demás del lote")
        void unaSinEntidadNoFrenaAlResto() {
            Donacion valida = donacionAsignada();
            Donacion huerfana = new Donacion();
            huerfana.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);

            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA"))
                    .thenReturn(List.of(huerfana, valida));
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(broker.planificar(anyList()))
                    .thenReturn(ResultadoPlanificacion.aceptada("propia", List.of()));

            servicio.planificarEntregasDelDiaSiguiente();

            verify(broker).planificar(entregasCaptor.capture());
            assertThat(entregasCaptor.getValue()).hasSize(1);
            assertThat(entregasCaptor.getValue().getFirst().idDonacion()).isEqualTo(valida.getId());
        }
    }

    @Nested
    @DisplayName("Efecto sobre el estado de las donaciones")
    class EfectoSobreElEstado {

        @Test
        @DisplayName("Las donaciones aceptadas pasan a LISTA_PARA_ENTREGAR")
        void aceptadasPasanAListaParaEntregar() {
            // Es lo que habilita el siguiente paso del circuito: el inicio de ruta
            // solo es una transición legal desde LISTA_PARA_ENTREGAR.
            Donacion donacion = donacionAsignada();
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).thenReturn(List.of(donacion));
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(broker.planificar(anyList()))
                    .thenReturn(ResultadoPlanificacion.aceptada("propia", List.of(donacion.getId())));

            servicio.planificarEntregasDelDiaSiguiente();

            assertThat(donacion.estaEnEstado("LISTA_PARA_ENTREGAR")).isTrue();
        }

        @Test
        @DisplayName("Si el proveedor NO aceptó, las donaciones siguen en ASIGNACION_REALIZADA")
        void rechazadasNoCambianDeEstado() {
            // Quedan disponibles para el intento de la noche siguiente.
            Donacion donacion = donacionAsignada();
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).thenReturn(List.of(donacion));
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(broker.planificar(anyList())).thenReturn(ResultadoPlanificacion.sinProveedorDisponible());

            servicio.planificarEntregasDelDiaSiguiente();

            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("Solo cambian de estado las donaciones que el proveedor aceptó")
        void soloLasAceptadas() {
            // El proveedor puede tomar parte del lote: las que quedaron afuera no
            // pueden marcarse como listas.
            Donacion aceptada = donacionAsignada();
            Donacion noAceptada = donacionAsignada();
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA"))
                    .thenReturn(List.of(aceptada, noAceptada));
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(broker.planificar(anyList()))
                    .thenReturn(ResultadoPlanificacion.aceptada("propia", List.of(aceptada.getId())));

            servicio.planificarEntregasDelDiaSiguiente();

            assertThat(aceptada.estaEnEstado("LISTA_PARA_ENTREGAR")).isTrue();
            assertThat(noAceptada.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("Un cambio de estado que falla no interrumpe al resto del lote")
        void unCambioFallidoNoFrenaElProceso() {
            // Si una donación ya avanzó por otra vía, su transición será ilegal.
            // Es un batch nocturno: tiene que seguir con las demás.
            Donacion normal = donacionAsignada();
            Donacion yaAvanzada = donacionAsignada();
            yaAvanzada.cambiarEstado("LISTA_PARA_ENTREGAR", "planificar", null);

            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA"))
                    .thenReturn(List.of(yaAvanzada, normal));
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(broker.planificar(anyList())).thenReturn(ResultadoPlanificacion.aceptada(
                    "propia", List.of(yaAvanzada.getId(), normal.getId())));

            assertThatCode(() -> servicio.planificarEntregasDelDiaSiguiente()).doesNotThrowAnyException();

            assertThat(normal.estaEnEstado("LISTA_PARA_ENTREGAR")).isTrue();
        }
    }
}
