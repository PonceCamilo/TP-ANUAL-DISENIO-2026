package ar.utn.donatrack.donaciones.integracion.logistica;

import ar.utn.donatrack.donaciones.interfaces.integracion.LogisticaPort;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.entidad.Localidad;
import ar.utn.donatrack.donaciones.models.entidad.Provincia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del broker de integración con logística (Entrega 4).
 *
 * El enunciado pide poder "seleccionar entre más de 1 servicio de logística
 * disponible". El criterio implementado es preferencia configurable con failover
 * automático, y estos tests fijan ese comportamiento:
 *
 *   - se respeta el proveedor preferido mientras esté disponible;
 *   - si no lo está, se pasa al siguiente sin intervención manual;
 *   - si el preferido acepta, no se molesta a los demás;
 *   - si ninguno puede, se informa el fracaso sin lanzar excepciones.
 *
 * Los proveedores van mockeados: acá no se prueba cómo habla cada adapter con su
 * servicio, sino a cuál de ellos decide derivarle el trabajo el broker.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrokerLogistica - selección del proveedor de logística")
class BrokerLogisticaTest {

    @Mock
    private LogisticaPort propia;

    @Mock
    private LogisticaPort tercerizada;

    private List<SolicitudEntrega> entregas;

    @BeforeEach
    void prepararEscenario() {
        lenient().when(propia.nombre()).thenReturn("propia");
        lenient().when(tercerizada.nombre()).thenReturn("tercerizada");

        entregas = List.of(unaEntrega(), unaEntrega());
    }

    private SolicitudEntrega unaEntrega() {
        Direccion direccion = Direccion.builder()
                .calle("Medrano")
                .numero(951)
                .codigoPostal("C1179")
                .localidad(Localidad.builder()
                        .nombre("CABA")
                        .provincia(Provincia.builder().nombre("Buenos Aires").build())
                        .build())
                .build();
        return new SolicitudEntrega(UUID.randomUUID(), UUID.randomUUID(), direccion);
    }

    /** Broker con los dos proveedores y el preferido indicado. */
    private BrokerLogistica brokerCon(String preferido) {
        return new BrokerLogistica(List.of(propia, tercerizada), preferido);
    }

    @Nested
    @DisplayName("Elección del proveedor preferido")
    class Preferencia {

        @Test
        @DisplayName("Con la preferencia en 'propia', se deriva al servicio propio")
        void usaElPreferidoPropia() {
            when(propia.estaDisponible()).thenReturn(true);
            when(propia.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.aceptada("propia", List.of()));

            ResultadoPlanificacion resultado = brokerCon("propia").planificar(entregas);

            assertThat(resultado.aceptada()).isTrue();
            assertThat(resultado.proveedor()).isEqualTo("propia");
            verify(tercerizada, never()).solicitarPlanificacion(anyList());
        }

        @Test
        @DisplayName("Cambiando la preferencia a 'tercerizada', se deriva al operador externo")
        void usaElPreferidoTercerizada() {
            // Misma lista de proveedores, solo cambia una property: eso es lo que
            // el enunciado pide poder hacer ("seleccionar entre más de 1 servicio").
            when(tercerizada.estaDisponible()).thenReturn(true);
            when(tercerizada.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.aceptada("tercerizada", List.of()));

            ResultadoPlanificacion resultado = brokerCon("tercerizada").planificar(entregas);

            assertThat(resultado.proveedor()).isEqualTo("tercerizada");
            verify(propia, never()).solicitarPlanificacion(anyList());
        }

        @Test
        @DisplayName("Si el preferido acepta, no se consulta la disponibilidad de los demás")
        void noMolestaAlResto() {
            when(propia.estaDisponible()).thenReturn(true);
            when(propia.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.aceptada("propia", List.of()));

            brokerCon("propia").planificar(entregas);

            verify(tercerizada, never()).estaDisponible();
        }

        @Test
        @DisplayName("Un nombre de preferencia desconocido no rompe: se usa el primero disponible")
        void preferenciaInexistente() {
            // Caso borde de configuración: alguien escribe mal el nombre en el
            // application.properties. El sistema sigue repartiendo igual.
            when(propia.estaDisponible()).thenReturn(true);
            when(propia.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.aceptada("propia", List.of()));

            assertThat(brokerCon("no-existe").planificar(entregas).aceptada()).isTrue();
        }
    }

    @Nested
    @DisplayName("Failover automático")
    class Failover {

        @Test
        @DisplayName("Si el preferido NO está disponible, se deriva al siguiente")
        void failoverPorIndisponibilidad() {
            // Escenario real: el servicio de logística propio está caído o sin
            // camiones libres. Las entregas no pueden quedar sin repartir.
            when(propia.estaDisponible()).thenReturn(false);
            when(tercerizada.estaDisponible()).thenReturn(true);
            when(tercerizada.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.aceptada("tercerizada", List.of()));

            ResultadoPlanificacion resultado = brokerCon("propia").planificar(entregas);

            assertThat(resultado.aceptada()).isTrue();
            assertThat(resultado.proveedor()).isEqualTo("tercerizada");
        }

        @Test
        @DisplayName("Si el preferido está disponible pero RECHAZA, se intenta con el siguiente")
        void failoverPorRechazo() {
            // Distinto de estar caído: responde, pero no puede tomar el trabajo
            // (por ejemplo, toda la flota está en ruta).
            when(propia.estaDisponible()).thenReturn(true);
            when(propia.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.rechazada("propia", "No hay camiones disponibles."));
            when(tercerizada.estaDisponible()).thenReturn(true);
            when(tercerizada.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.aceptada("tercerizada", List.of()));

            ResultadoPlanificacion resultado = brokerCon("propia").planificar(entregas);

            assertThat(resultado.proveedor()).isEqualTo("tercerizada");
        }

        @Test
        @DisplayName("Si NINGÚN proveedor puede, informa el fracaso sin lanzar excepción")
        void ningunoDisponible() {
            // El disparador es un batch nocturno: necesita registrar el problema y
            // seguir, no caerse. Las donaciones quedan para el intento de mañana.
            when(propia.estaDisponible()).thenReturn(false);
            when(tercerizada.estaDisponible()).thenReturn(false);

            ResultadoPlanificacion resultado = brokerCon("propia").planificar(entregas);

            assertThat(resultado.aceptada()).isFalse();
            assertThat(resultado.proveedor()).isEqualTo("ninguno");
            assertThat(resultado.idsDonaciones()).isEmpty();
        }

        @Test
        @DisplayName("Si todos rechazan, también se informa el fracaso")
        void todosRechazan() {
            when(propia.estaDisponible()).thenReturn(true);
            when(propia.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.rechazada("propia", "sin camiones"));
            when(tercerizada.estaDisponible()).thenReturn(true);
            when(tercerizada.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.rechazada("tercerizada", "fuera de zona"));

            assertThat(brokerCon("propia").planificar(entregas).aceptada()).isFalse();
        }
    }

    @Nested
    @DisplayName("Casos borde")
    class CasosBorde {

        @Test
        @DisplayName("Sin donaciones que planificar, no se molesta a ningún proveedor")
        void sinEntregas() {
            ResultadoPlanificacion resultado = brokerCon("propia").planificar(List.of());

            assertThat(resultado.aceptada()).isFalse();
            verify(propia, never()).estaDisponible();
            verify(tercerizada, never()).estaDisponible();
        }

        @Test
        @DisplayName("Sin ningún proveedor registrado, informa el fracaso sin romper")
        void sinProveedores() {
            BrokerLogistica brokerVacio = new BrokerLogistica(List.of(), "propia");

            ResultadoPlanificacion resultado = brokerVacio.planificar(entregas);

            assertThat(resultado.aceptada()).isFalse();
            assertThat(resultado.detalle()).contains("Ningún proveedor");
        }

        @Test
        @DisplayName("Funciona con un solo proveedor registrado (el escenario actual del sistema)")
        void unSoloProveedor() {
            // Hoy el operador tercerizado viene deshabilitado, así que el broker
            // arranca con un único adapter. Tiene que seguir funcionando igual.
            BrokerLogistica brokerConUno = new BrokerLogistica(List.of(propia), "propia");
            when(propia.estaDisponible()).thenReturn(true);
            when(propia.solicitarPlanificacion(entregas))
                    .thenReturn(ResultadoPlanificacion.aceptada("propia", List.of()));

            assertThat(brokerConUno.planificar(entregas).aceptada()).isTrue();
        }

        @Test
        @DisplayName("Expone qué proveedores quedaron registrados, para diagnóstico")
        void listaLosProveedores() {
            assertThat(brokerCon("propia").proveedoresRegistrados())
                    .containsExactlyInAnyOrder("propia", "tercerizada");
        }
    }
}
