package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.clientes.BrokerLogisticaClient;
import ar.utn.donatrack.donaciones.dtos.response.EnvioLogisticaResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.logisticaExceptions.DonacionNoDespachableException;
import ar.utn.donatrack.donaciones.exceptions.logisticaExceptions.LogisticaNoDisponibleException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.Localidad;
import ar.utn.donatrack.donaciones.models.entidad.Provincia;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests del despacho de donaciones al broker de logística (Entrega 4).
 *
 * Donaciones no elige el proveedor ni habla con los servicios de logística:
 * manda el lote al broker (8087) y aplica lo que el broker devuelve.
 *
 *   matchmaking ─▶ el administrador confirma ─▶ POST /donaciones/envios
 *                     ─▶ broker ─▶ DONATRACK (8085) o EXTERNA (8086)
 *
 * Lo que se verifica: la validación previa (todo o nada), la traducción del
 * pedido al contrato del broker, qué se guarda de la respuesta, y que un fallo
 * del broker NO deje las donaciones en un estado inconsistente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanificacionEntregasService - despacho al broker de logística")
class PlanificacionEntregasServiceTest {

    @Mock
    private DonacionesRepositoryInterface donacionesRepositorio;

    @Mock
    private EntidadesBeneficiariasRepositoryInterface entidadesRepositorio;

    @Mock
    private BrokerLogisticaClient brokerClient;

    @Captor
    private ArgumentCaptor<List<BrokerLogisticaClient.DonacionEnvio>> pedidoCaptor;

    private PlanificacionEntregasService servicio;
    private EntidadBeneficiaria entidad;

    @BeforeEach
    void prepararEscenario() {
        servicio = new PlanificacionEntregasService(
                donacionesRepositorio,
                entidadesRepositorio,
                brokerClient,
                new DonacionesValidator(donacionesRepositorio));

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

    /** Donación en ASIGNACION_REALIZADA, registrada en el repositorio mock. */
    private Donacion donacionAsignada() {
        Donacion donacion = new Donacion();
        donacion.setSubcategoria(new Subcategoria("arroz"));
        donacion.setIdDonante(UUID.randomUUID());
        donacion.asignarA(entidad);

        lenient().when(donacionesRepositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
        lenient().when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
        return donacion;
    }

    private BrokerLogisticaClient.ResultadoEnvio respuestaBroker(
            String proveedor, List<String> descartados, Donacion... aceptadas) {
        List<BrokerLogisticaClient.EnvioAsignado> envios = List.of(aceptadas).stream()
                .map(d -> new BrokerLogisticaClient.EnvioAsignado(
                        d.getId(), proveedor, "SEG-" + d.getId().toString().substring(0, 8),
                        LocalDateTime.of(2026, 10, 7, 11, 26)))
                .toList();
        return new BrokerLogisticaClient.ResultadoEnvio(proveedor, descartados, envios);
    }

    @Nested
    @DisplayName("Validación previa: todo o nada")
    class ValidacionPrevia {

        @Test
        @DisplayName("Si alguna donación no existe, no se llama al broker")
        void donacionInexistente() {
            // Deliberadamente no se despacha un lote a medias: parte en logística
            // y parte sin despachar sería imposible de reconciliar.
            Donacion valida = donacionAsignada();
            UUID inexistente = UUID.randomUUID();
            when(donacionesRepositorio.obtenerPorId(inexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.despachar(List.of(valida.getId(), inexistente), null))
                    .isInstanceOf(DonacionNoEncontradaException.class);

            verifyNoInteractions(brokerClient);
        }

        @Test
        @DisplayName("Si alguna donación no está en ASIGNACION_REALIZADA, se rechaza el lote entero")
        void estadoIncorrecto() {
            Donacion valida = donacionAsignada();
            Donacion enDeposito = new Donacion();
            when(donacionesRepositorio.obtenerPorId(enDeposito.getId())).thenReturn(enDeposito);

            assertThatThrownBy(() -> servicio.despachar(List.of(valida.getId(), enDeposito.getId()), null))
                    .isInstanceOf(DonacionNoDespachableException.class)
                    .hasMessageContaining("EN_DEPOSITO");

            verifyNoInteractions(brokerClient);
            assertThat(valida.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("Si la entidad destino no tiene dirección, se rechaza antes de llamar al broker")
        void entidadSinDireccion() {
            // El broker exige calle, localidad y provincia (@NotBlank): mandarlo
            // así solo conseguiría un 400 del otro lado.
            Donacion donacion = donacionAsignada();
            EntidadBeneficiaria sinDireccion = EntidadBeneficiaria.builder()
                    .id(entidad.getId())
                    .razonSocial("Comedor sin dirección")
                    .build();
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(sinDireccion);

            assertThatThrownBy(() -> servicio.despachar(List.of(donacion.getId()), null))
                    .isInstanceOf(DonacionNoDespachableException.class)
                    .hasMessageContaining("dirección");

            verifyNoInteractions(brokerClient);
        }

        @Test
        @DisplayName("Una lista vacía se rechaza sin llamar al broker")
        void listaVacia() {
            assertThatThrownBy(() -> servicio.despachar(List.of(), null))
                    .isInstanceOf(DonacionNoDespachableException.class);

            verifyNoInteractions(brokerClient);
        }
    }

    @Nested
    @DisplayName("Traducción del pedido al contrato del broker")
    class TraduccionDelPedido {

        @Test
        @DisplayName("Aplana la dirección anidada al formato plano que espera el broker")
        void aplanaLaDireccion() {
            // La Direccion de donaciones anida Localidad → Provincia; el DireccionDTO
            // del broker es plano. Si el aplanado falla, el broker responde 400.
            Donacion donacion = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("DONATRACK", List.of(), donacion));

            servicio.despachar(List.of(donacion.getId()), null);

            verify(brokerClient).solicitarEnvio(isNull(), pedidoCaptor.capture());
            BrokerLogisticaClient.DonacionEnvio enviado = pedidoCaptor.getValue().getFirst();

            assertThat(enviado.idDonacion()).isEqualTo(donacion.getId());
            assertThat(enviado.idEntidadBeneficiaria()).isEqualTo(entidad.getId());
            assertThat(enviado.nombreEntidad()).isEqualTo("Comedor Los Pibes");
            assertThat(enviado.direccionEntrega().calle()).isEqualTo("Medrano");
            assertThat(enviado.direccionEntrega().numero()).isEqualTo(951);
            assertThat(enviado.direccionEntrega().localidad()).isEqualTo("CABA");
            assertThat(enviado.direccionEntrega().provincia()).isEqualTo("Buenos Aires");
            assertThat(enviado.direccionEntrega().codigoPostal()).isEqualTo("C1179");
        }

        @Test
        @DisplayName("Manda todas las donaciones del lote en un solo pedido")
        void unSoloPedidoPorLote() {
            // Logística planifica rutas por lotes: mandarlas de a una haría que
            // cada donación termine en su propia ruta.
            Donacion primera = donacionAsignada();
            Donacion segunda = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("DONATRACK", List.of(), primera, segunda));

            servicio.despachar(List.of(primera.getId(), segunda.getId()), null);

            verify(brokerClient).solicitarEnvio(isNull(), pedidoCaptor.capture());
            assertThat(pedidoCaptor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("Propaga el proveedor forzado cuando se pide uno puntual")
        void proveedorForzado() {
            Donacion donacion = donacionAsignada();
            when(brokerClient.solicitarEnvio(eq("EXTERNA"), anyList()))
                    .thenReturn(respuestaBroker("EXTERNA", List.of(), donacion));

            servicio.despachar(List.of(donacion.getId()), "EXTERNA");

            verify(brokerClient).solicitarEnvio(eq("EXTERNA"), anyList());
        }
    }

    @Nested
    @DisplayName("Qué se hace con la respuesta del broker")
    class ProcesamientoDeLaRespuesta {

        @Test
        @DisplayName("Las donaciones aceptadas pasan a LISTA_PARA_ENTREGAR con proveedor y seguimiento")
        void guardaProveedorYSeguimiento() {
            // El paso de estado es obligatorio: sin él, el inicio de ruta fallaría
            // con una transición ilegal (ASIGNACION_REALIZADA → EN_TRASLADO).
            Donacion donacion = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("DONATRACK", List.of(), donacion));

            servicio.despachar(List.of(donacion.getId()), null);

            assertThat(donacion.estaEnEstado("LISTA_PARA_ENTREGAR")).isTrue();
            assertThat(donacion.getProveedorLogistica()).isEqualTo("DONATRACK");
            assertThat(donacion.getIdSeguimientoLogistica()).isNotBlank();
        }

        @Test
        @DisplayName("PERSISTE el despacho")
        void persisteElDespacho() {
            // Mismo motivo que en DonacionService: sin guardar, con JPA el
            // proveedor y el id de seguimiento no llegan nunca a la base.
            Donacion donacion = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("DONATRACK", List.of(), donacion));

            servicio.despachar(List.of(donacion.getId()), null);

            verify(donacionesRepositorio).guardar(donacion);
        }

        @Test
        @DisplayName("El historial registra qué proveedor tomó la donación")
        void historialRegistraElProveedor() {
            Donacion donacion = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("EXTERNA", List.of(), donacion));

            servicio.despachar(List.of(donacion.getId()), null);

            assertThat(donacion.getHistorialEstados().getLast().getJustificacion())
                    .contains("EXTERNA")
                    .contains("seguimiento");
        }

        @Test
        @DisplayName("La respuesta expone los proveedores descartados, que es el fallback en acción")
        void exponeElFallback() {
            Donacion donacion = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("EXTERNA", List.of("DONATRACK"), donacion));

            EnvioLogisticaResponseDTO resultado = servicio.despachar(List.of(donacion.getId()), null);

            assertThat(resultado.getProveedor()).isEqualTo("EXTERNA");
            assertThat(resultado.getProveedoresDescartados()).containsExactly("DONATRACK");
            assertThat(resultado.getEnvios()).hasSize(1);
            assertThat(resultado.getEnvios().getFirst().getIdDonacion()).isEqualTo(donacion.getId());
        }

        @Test
        @DisplayName("Si el broker devuelve menos envíos que lo pedido, solo avanzan los aceptados")
        void respuestaParcial() {
            Donacion aceptada = donacionAsignada();
            Donacion noAceptada = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("DONATRACK", List.of(), aceptada));

            servicio.despachar(List.of(aceptada.getId(), noAceptada.getId()), null);

            assertThat(aceptada.estaEnEstado("LISTA_PARA_ENTREGAR")).isTrue();
            assertThat(noAceptada.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("Un envío para una donación que no estaba en el pedido se ignora sin romper")
        void envioDesconocidoSeIgnora() {
            // Caso defensivo: no confiamos ciegamente en que la respuesta del
            // broker se corresponda con lo que pedimos.
            Donacion donacion = donacionAsignada();
            BrokerLogisticaClient.ResultadoEnvio conExtra = new BrokerLogisticaClient.ResultadoEnvio(
                    "DONATRACK", List.of(),
                    List.of(new BrokerLogisticaClient.EnvioAsignado(
                            UUID.randomUUID(), "DONATRACK", "SEG-XXX", LocalDateTime.now())));
            when(brokerClient.solicitarEnvio(isNull(), anyList())).thenReturn(conExtra);

            assertThatCode(() -> servicio.despachar(List.of(donacion.getId()), null))
                    .doesNotThrowAnyException();

            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }
    }

    @Nested
    @DisplayName("Fallas del broker: las donaciones quedan intactas")
    class FallasDelBroker {

        @Test
        @DisplayName("Si el broker no responde, se lanza LogisticaNoDisponible y nada cambia de estado")
        void brokerCaido() {
            // Es el caso que justifica que este cliente SÍ propague el error, al
            // contrario de NotificacionClient: si el envío no salió, la donación
            // no puede quedar marcada como lista para entregar.
            Donacion donacion = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> servicio.despachar(List.of(donacion.getId()), null))
                    .isInstanceOf(LogisticaNoDisponibleException.class);

            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
            assertThat(donacion.getProveedorLogistica()).isNull();
        }

        @Test
        @DisplayName("Si el broker responde sin envíos, también se trata como no disponible")
        void respuestaSinEnvios() {
            Donacion donacion = donacionAsignada();
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(new BrokerLogisticaClient.ResultadoEnvio("ninguno", List.of(), null));

            assertThatThrownBy(() -> servicio.despachar(List.of(donacion.getId()), null))
                    .isInstanceOf(LogisticaNoDisponibleException.class);

            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }
    }

    @Nested
    @DisplayName("Barrido nocturno")
    class BarridoNocturno {

        @Test
        @DisplayName("Despacha todo lo que quedó en ASIGNACION_REALIZADA")
        void despachaLosPendientes() {
            Donacion donacion = donacionAsignada();
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).thenReturn(List.of(donacion));
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("DONATRACK", List.of(), donacion));

            servicio.despacharPendientesDelDia();

            assertThat(donacion.estaEnEstado("LISTA_PARA_ENTREGAR")).isTrue();
        }

        @Test
        @DisplayName("Saltea las donaciones sin dirección resoluble en vez de fallar")
        void salteaLasNoDespachables() {
            // A diferencia del endpoint, el barrido es desatendido: no puede
            // cortarse porque una entidad quedó sin dirección.
            Donacion despachable = donacionAsignada();
            Donacion huerfana = new Donacion();
            huerfana.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);

            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA"))
                    .thenReturn(List.of(huerfana, despachable));
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenReturn(respuestaBroker("DONATRACK", List.of(), despachable));

            servicio.despacharPendientesDelDia();

            verify(brokerClient).solicitarEnvio(isNull(), pedidoCaptor.capture());
            assertThat(pedidoCaptor.getValue()).hasSize(1);
            assertThat(despachable.estaEnEstado("LISTA_PARA_ENTREGAR")).isTrue();
        }

        @Test
        @DisplayName("Sin donaciones pendientes no llama al broker")
        void sinPendientes() {
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).thenReturn(List.of());

            servicio.despacharPendientesDelDia();

            verify(brokerClient, never()).solicitarEnvio(any(), anyList());
        }

        @Test
        @DisplayName("Si el broker falla, el barrido no propaga la excepción")
        void brokerCaidoNoRompeElBarrido() {
            // Es un @Scheduled: una excepción que escape solo ensucia los logs y
            // no la ve nadie. Las donaciones entran de nuevo mañana.
            Donacion donacion = donacionAsignada();
            when(donacionesRepositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).thenReturn(List.of(donacion));
            when(brokerClient.solicitarEnvio(isNull(), anyList()))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            assertThatCode(() -> servicio.despacharPendientesDelDia()).doesNotThrowAnyException();

            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }
    }
}
