package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.dtos.request.EntregaExitosaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.EntregaFallidaCallbackDTO;
import ar.utn.donatrack.donaciones.dtos.request.InicioRutaCallbackDTO;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests del receptor de eventos del Servicio de Logística.
 *
 * Regla arquitectónica de la Entrega 3 que este servicio materializa: Logística
 * NO llama directamente a notificaciones ni a incentivos. Publica el evento y es
 * el Servicio de Donaciones quien actualiza el estado de la donación y orquesta
 * los avisos. Por eso todos los tests verifican DOS cosas por evento: el cambio
 * de estado de la donación y las notificaciones que se disparan.
 *
 * Los tres eventos del circuito:
 *   - Inicio de ruta   -> EN_TRASLADO      + aviso con link al mapa en vivo
 *   - Entrega exitosa  -> ENTREGADA        + comprobante + aviso a incentivos
 *   - Entrega fallida  -> ENTREGA_FALLIDA  + aviso a entidad, donante y admin
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogisticaEventosService - eventos de logística y notificaciones")
class LogisticaEventosServiceTest {

    private static final String EMAIL_ENTIDAD = "comedor@lospibes.org";
    private static final String EMAIL_DONANTE = "juan@example.com";
    private static final String EMAIL_ADMIN = "admin@donatrack.org";
    private static final String URL_MAPA = "https://donatrack.org/mapa/ruta-123";

    @Mock
    private RepositorioDonaciones donacionesRepositorio;

    @Mock
    private RepositorioEntidadesBeneficiarias entidadesRepositorio;

    @Mock
    private RepositorioPersonasDonantes donanteRepositorio;

    private final NotificacionClient notificacionClient = mock(NotificacionClient.class);

    @Spy
    private Notificador notificador = new Notificador(notificacionClient, DonacionesProperties.porDefecto());

    @Mock
    private IncentivosClient incentivosClient;

    @Spy
    private DonacionesProperties propiedades = DonacionesProperties.porDefecto();

    @InjectMocks
    private LogisticaEventosService servicio;

    private Donacion donacion;
    private EntidadBeneficiaria entidad;
    private final UUID idDonante = UUID.randomUUID();

    @BeforeEach
    void prepararEscenario() {
        entidad = EntidadBeneficiaria.builder()
                .id(UUID.randomUUID())
                .razonSocial("Comedor Los Pibes")
                .contactos(List.of(Email.builder().valor(EMAIL_ENTIDAD).build()))
                .campanias(new ArrayList<>())
                .build();

        donacion = new Donacion();
        donacion.setIdDonante(idDonante);
        donacion.setSubcategoria(new Subcategoria("arroz"));
        donacion.setIdEntidadBeneficiaria(entidad.getId());
    }

    private PersonaDonante donanteConEmail(String email) {
        return PersonaHumana.builder()
                .id(idDonante)
                .nombre("Juan")
                .email(email)
                .build();
    }

    /** Lleva la donación hasta LISTA_PARA_ENTREGAR, el estado previo al inicio de ruta. */
    private void prepararParaInicioDeRuta() {
        donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
        donacion.cambiarEstado("LISTA_PARA_ENTREGAR", "planificar", null);
    }

    /** Lleva la donación hasta EN_TRASLADO, el estado previo a la entrega. */
    private void prepararParaEntrega() {
        prepararParaInicioDeRuta();
        donacion.cambiarEstado("EN_TRASLADO", "iniciar ruta", null);
    }

    @Nested
    @DisplayName("Inicio de ruta")
    class InicioDeRuta {

        private InicioRutaCallbackDTO evento(UUID... idsDonaciones) {
            return new InicioRutaCallbackDTO(UUID.randomUUID(), List.of(idsDonaciones), URL_MAPA);
        }

        @Test
        @DisplayName("Pasa la donación a EN_TRASLADO y registra la ruta en el historial")
        void pasaAEnTraslado() {
            prepararParaInicioDeRuta();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarInicioRuta(evento(donacion.getId()));

            assertThat(donacion.estaEnEstado("EN_TRASLADO")).isTrue();
            assertThat(donacion.getHistorialEstados().getLast().getJustificacion()).contains("Ruta:");
        }

        @Test
        @DisplayName("Notifica a la entidad y al donante con el link al mapa en tiempo real")
        void notificaConElLinkAlMapa() {
            // El enunciado pide que ambas partes puedan seguir la entrega en vivo.
            prepararParaInicioDeRuta();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarInicioRuta(evento(donacion.getId()));

            verify(notificacionClient).enviarNotificacion(eq(EMAIL_ENTIDAD), contains(URL_MAPA), eq("EMAIL"));
            verify(notificacionClient).enviarNotificacion(eq(EMAIL_DONANTE), contains(URL_MAPA), eq("EMAIL"));
        }

        @Test
        @DisplayName("Una ruta con varias donaciones las procesa a todas")
        void procesaTodasLasDonacionesDeLaRuta() {
            // Un camión lleva varias donaciones: el evento llega una sola vez con
            // la lista completa y todas deben pasar a EN_TRASLADO.
            Donacion segunda = new Donacion();
            segunda.setIdDonante(idDonante);
            segunda.setIdEntidadBeneficiaria(entidad.getId());
            prepararParaInicioDeRuta();
            segunda.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
            segunda.cambiarEstado("LISTA_PARA_ENTREGAR", "planificar", null);

            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(donacionesRepositorio.buscarPorId(segunda.getId())).thenReturn(Optional.of(segunda));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarInicioRuta(evento(donacion.getId(), segunda.getId()));

            assertThat(donacion.estaEnEstado("EN_TRASLADO")).isTrue();
            assertThat(segunda.estaEnEstado("EN_TRASLADO")).isTrue();
            verify(notificacionClient, times(4)).enviarNotificacion(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Lanza 404 si alguna donación del evento no existe")
        void donacionInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(donacionesRepositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.procesarInicioRuta(evento(idInexistente)))
                    .isInstanceOf(DonacionNoEncontradaException.class);
        }

        @Test
        @DisplayName("Una donación que no llegó a LISTA_PARA_ENTREGAR no puede pasar a EN_TRASLADO")
        void estadoIncompatible() {
            // Protege la trazabilidad: logística no puede saltear la planificación.
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            assertThatThrownBy(() -> servicio.procesarInicioRuta(evento(donacion.getId())))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);
            verifyNoInteractions(notificacionClient);
        }

        @Test
        @DisplayName("Si la entidad no tiene email se le avisa por su otro contacto (SMS), y al donante igual")
        void entidadSinEmail() {
            prepararParaInicioDeRuta();
            EntidadBeneficiaria sinEmail = EntidadBeneficiaria.builder()
                    .id(entidad.getId())
                    .razonSocial("Comedor Los Pibes")
                    .contactos(List.of(Telefono.builder().valor("1155667788").build()))
                    .build();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(sinEmail));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarInicioRuta(evento(donacion.getId()));

            verify(notificacionClient).enviarNotificacion(eq("1155667788"), anyString(), eq("SMS"));
            verify(notificacionClient).enviarNotificacion(eq(EMAIL_DONANTE), anyString(), anyString());
        }

        @Test
        @DisplayName("Una donación sin entidad asignada solo notifica al donante")
        void donacionSinEntidad() {
            prepararParaInicioDeRuta();
            donacion.setIdEntidadBeneficiaria(null);
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarInicioRuta(evento(donacion.getId()));

            verify(notificacionClient, times(1)).enviarNotificacion(eq(EMAIL_DONANTE), anyString(), anyString());
            verifyNoInteractions(entidadesRepositorio);
        }
    }

    @Nested
    @DisplayName("Entrega exitosa")
    class EntregaExitosa {

        private EntregaExitosaCallbackDTO evento() {
            return new EntregaExitosaCallbackDTO(
                    donacion.getId(), UUID.randomUUID(), "AB123CD",
                    LocalDateTime.of(2026, 3, 15, 14, 30));
        }

        @Test
        @DisplayName("Pasa la donación a ENTREGADA registrando patente y fecha en el historial")
        void pasaAEntregada() {
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarEntregaExitosa(evento());

            assertThat(donacion.fueEntregada()).isTrue();
            assertThat(donacion.getHistorialEstados().getLast().getJustificacion()).contains("AB123CD");
        }

        @Test
        @DisplayName("Envía a entidad y donante el comprobante con patente y fecha de entrega")
        void enviaElComprobante() {
            // El enunciado pide un comprobante con fecha, hora y patente del camión.
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarEntregaExitosa(evento());

            verify(notificacionClient).enviarNotificacion(eq(EMAIL_ENTIDAD), contains("AB123CD"), eq("EMAIL"));
            verify(notificacionClient).enviarNotificacion(eq(EMAIL_DONANTE), contains("AB123CD"), eq("EMAIL"));
        }

        @Test
        @DisplayName("Avisa a incentivos, porque las donaciones exitosas alimentan las insignias")
        void avisaAIncentivos() {
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarEntregaExitosa(evento());

            // Se informa la fecha de la entrega, no la del registro de la donación.
            verify(incentivosClient).notificarDonacionExitosa(donacion, EMAIL_DONANTE, "EMAIL", LocalDateTime.of(2026, 3, 15, 14, 30));
        }

        @Test
        @DisplayName("Si el donante no tiene email, la entrega se registra igual y no se avisa a incentivos")
        void donanteSinEmail() {
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(null)));

            servicio.procesarEntregaExitosa(evento());

            assertThat(donacion.fueEntregada()).isTrue();
            verifyNoInteractions(incentivosClient);
        }

        @Test
        @DisplayName("Lanza 404 si la donación del evento no existe")
        void donacionInexistente() {
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.procesarEntregaExitosa(evento()))
                    .isInstanceOf(DonacionNoEncontradaException.class);
        }

        @Test
        @DisplayName("Una donación que no está EN_TRASLADO no puede confirmarse como entregada")
        void estadoIncompatible() {
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            assertThatThrownBy(() -> servicio.procesarEntregaExitosa(evento()))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);
        }
    }

    @Nested
    @DisplayName("Entrega fallida")
    class EntregaFallida {

        private EntregaFallidaCallbackDTO evento(boolean replanificable) {
            return new EntregaFallidaCallbackDTO(
                    donacion.getId(), "Tocamos timbre pero nadie respondió", replanificable);
        }

        @Test
        @DisplayName("Pasa la donación a ENTREGA_FALLIDA guardando el motivo como justificación")
        void pasaAEntregaFallida() {
            // El motivo es obligatorio: la máquina de estados rechaza la transición
            // sin justificación, y acá se comprueba que el servicio la provea.
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarEntregaFallida(evento(true));

            assertThat(donacion.estaEnEstado("ENTREGA_FALLIDA")).isTrue();
            assertThat(donacion.getHistorialEstados().getLast().getJustificacion())
                    .contains("Tocamos timbre pero nadie respondió");
        }

        @Test
        @DisplayName("La justificación deja registrado si la donación puede replanificarse")
        void registraSiEsReplanificable() {
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarEntregaFallida(evento(false));

            assertThat(donacion.getHistorialEstados().getLast().getJustificacion())
                    .contains("No puede ser replanificada");
        }

        @Test
        @DisplayName("Notifica a la entidad, al donante Y al administrador del sistema")
        void notificaALasTresPartes() {
            // La entrega fallida es el único evento que además escala al administrador,
            // porque requiere una decisión operativa (replanificar o no).
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarEntregaFallida(evento(true));

            verify(notificacionClient).enviarNotificacion(
                    eq(EMAIL_ENTIDAD), contains("Tocamos timbre"), eq("EMAIL"));
            verify(notificacionClient).enviarNotificacion(
                    eq(EMAIL_DONANTE), contains("Tocamos timbre"), eq("EMAIL"));
            verify(notificacionClient).enviarNotificacion(
                    eq(EMAIL_ADMIN), contains("ALERTA"), eq("EMAIL"));
        }

        @Test
        @DisplayName("El aviso al administrador incluye el id de la donación afectada")
        void avisoAlAdminIdentificaLaDonacion() {
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarEntregaFallida(evento(true));

            verify(notificacionClient).enviarNotificacion(
                    eq(EMAIL_ADMIN), contains(donacion.getId().toString()), eq("EMAIL"));
        }

        @Test
        @DisplayName("Al administrador se le avisa aunque falten los emails de entidad y donante")
        void adminSiempreRecibeElAviso() {
            prepararParaEntrega();
            donacion.setIdEntidadBeneficiaria(null);
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(null)));

            servicio.procesarEntregaFallida(evento(true));

            verify(notificacionClient, times(1)).enviarNotificacion(anyString(), anyString(), anyString());
            verify(notificacionClient).enviarNotificacion(eq(EMAIL_ADMIN), anyString(), eq("EMAIL"));
        }

        @Test
        @DisplayName("Una donación que no está EN_TRASLADO no puede marcarse como fallida")
        void estadoIncompatible() {
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            assertThatThrownBy(() -> servicio.procesarEntregaFallida(evento(true)))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);
            verify(notificacionClient, never()).enviarNotificacion(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Después de fallar, la donación puede volver al depósito para replanificarse")
        void puedeVolverAlDeposito() {
            // Cierra el circuito del enunciado: una entrega fallida replanificable
            // reingresa al depósito y vuelve a estar disponible.
            prepararParaEntrega();
            when(donacionesRepositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donanteRepositorio.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(EMAIL_DONANTE)));

            servicio.procesarEntregaFallida(evento(true));
            donacion.cambiarEstado("EN_DEPOSITO", "replanificar", null);

            assertThat(donacion.estaEnEstado("EN_DEPOSITO")).isTrue();
        }
    }
}
