package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.config.PropiedadesDePrueba;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Whatsapp;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests de la tarea calendarizada que detecta donantes inactivos.
 *
 * El enunciado (Entrega 2) pide notificar a los donantes que llevan mucho tiempo
 * sin participar para incentivarlos a volver a donar. La tarea corre todos los
 * días a medianoche (@Scheduled) y el umbral es de 20 días sin interacción.
 *
 * El mensaje se envía por el medio de contacto PREDETERMINADO del donante, que
 * es el que el enunciado exige respetar: si eligió WhatsApp, no se le manda un
 * mail. Por eso se prueba el mapeo de cada tipo de medio a su canal.
 *
 * El método se invoca directamente (sin levantar el scheduler) para que el test
 * sea rápido y determinístico.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InactividadDonantesService - notificación a donantes inactivos")
class InactividadDonantesServiceTest {

    private static final int DIAS_LIMITE = 20;

    @Mock
    private RepositorioPersonasDonantes donanteRepository;

    private final NotificacionClient notificacionClient = mock(NotificacionClient.class);

    @Spy
    private Notificador notificador = new Notificador(notificacionClient, PropiedadesDePrueba.porDefecto());

    @Spy
    private DonacionesProperties propiedades = PropiedadesDePrueba.porDefecto();

    @Spy
    private TransactionOperations transaccion = TransactionOperations.withoutTransaction();

    @InjectMocks
    private InactividadDonantesService servicio;

    /**
     * Donante con la última interacción hace `diasSinInteractuar` días
     * y el medio de contacto predeterminado indicado.
     */
    private PersonaDonante donante(int diasSinInteractuar, MedioDeContacto medioPredeterminado) {
        PersonaHumana persona = PersonaHumana.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email("juan@example.com")
                .ultimaInteraccion(FechaHoraArgentina.ahora().minusDays(diasSinInteractuar))
                .build();
        if (medioPredeterminado != null) {
            persona.definirContactoPredeterminado(medioPredeterminado);
        }
        return persona;
    }

    @Nested
    @DisplayName("Detección de inactividad (umbral de 20 días)")
    class DeteccionDeInactividad {

        @Test
        @DisplayName("Notifica al donante que lleva más de 20 días sin interactuar")
        void notificaAlInactivo() {
            PersonaDonante inactivo = donante(DIAS_LIMITE + 10, Email.builder().valor("juan@example.com").build());
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(inactivo), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("juan@example.com"), anyString(), eq("EMAIL"));
        }

        @Test
        @DisplayName("Le pide a la base los donantes sin interacción desde hace 20 días")
        void consultaConElUmbralDe20Dias() {
            // El filtro por fecha lo hace la base con el índice de ultima_interaccion
            // (Entrega 5); su semántica se prueba en PersonaDonanteRepositoryTest.
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(), List.of());
            LocalDateTime antes = FechaHoraArgentina.ahora().minusDays(DIAS_LIMITE);

            servicio.notificarDonantesInactivos();

            ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(donanteRepository).obtenerParaAvisoDeInactividad(limite.capture(), anyInt());
            assertThat(limite.getValue()).isBetween(antes, FechaHoraArgentina.ahora().minusDays(DIAS_LIMITE));
            verifyNoInteractions(notificacionClient);
        }

        @Test
        @DisplayName("Notifica al donante que nunca interactuó (última interacción nula)")
        void notificaAlQueNuncaInteractuo() {
            // Caso típico de los donantes importados por CSV: existen en el sistema
            // pero todavía no hicieron nada, así que entran al circuito de reactivación.
            PersonaDonante nuevo = PersonaHumana.builder()
                    .id(UUID.randomUUID())
                    .build();
            nuevo.definirContactoPredeterminado(Email.builder().valor("nuevo@example.com").build());
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(nuevo), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("nuevo@example.com"), anyString(), eq("EMAIL"));
        }

        @Test
        @DisplayName("Notifica a todos los inactivos que devuelve la base")
        void notificaATodosLosInactivos() {
            PersonaDonante inactivoUno = donante(30, Email.builder().valor("inactivo1@example.com").build());
            PersonaDonante inactivoDos = donante(60, Email.builder().valor("inactivo2@example.com").build());
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(inactivoUno, inactivoDos), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("inactivo1@example.com"), anyString(), anyString());
            verify(notificacionClient).enviarNotificacion(eq("inactivo2@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("Sin donantes cargados la tarea corre sin hacer nada")
        void sinDonantesNoHaceNada() {
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(), List.of());

            servicio.notificarDonantesInactivos();

            verifyNoInteractions(notificacionClient);
        }
    }

    @Nested
    @DisplayName("Elección del canal según el medio predeterminado")
    class EleccionDelCanal {

        @Test
        @DisplayName("Un donante con Email predeterminado se notifica por EMAIL")
        void canalEmail() {
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(donante(30, Email.builder().valor("juan@example.com").build())), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("juan@example.com"), anyString(), eq("EMAIL"));
        }

        @Test
        @DisplayName("Un donante con Teléfono predeterminado se notifica por SMS")
        void canalSms() {
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(donante(30, Telefono.builder().valor("1155667788").build())), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("1155667788"), anyString(), eq("SMS"));
        }

        @Test
        @DisplayName("Un donante con WhatsApp predeterminado se notifica por WHATSAPP")
        void canalWhatsapp() {
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(donante(30, Whatsapp.builder().valor("1155667788").build())), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("1155667788"), anyString(), eq("WHATSAPP"));
        }

        @Test
        @DisplayName("Un donante sin ningún canal se omite sin romper la tarea")
        void sinMedioSeOmite() {
            // Importante: la tarea es batch. Un donante sin ningún canal no puede
            // impedir que se notifique a los demás.
            PersonaDonante sinMedio = PersonaHumana.builder().id(UUID.randomUUID()).build();
            PersonaDonante conMedio = donante(30, Email.builder().valor("ok@example.com").build());
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(sinMedio, conMedio), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient, times(1)).enviarNotificacion(anyString(), anyString(), anyString());
            verify(notificacionClient).enviarNotificacion(eq("ok@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("Sin canal predeterminado se le escribe al email de la persona")
        void sinPredeterminadoUsaElEmail() {
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(donante(30, null)), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("juan@example.com"), anyString(), eq("EMAIL"));
        }
    }

    @Nested
    @DisplayName("Contenido del mensaje")
    class ContenidoDelMensaje {

        @Test
        @DisplayName("El mensaje menciona los días de inactividad e invita a volver a donar")
        void mensajeInvitaADonar() {
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(donante(30, Email.builder().valor("juan@example.com").build())), List.of());

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(
                    anyString(), contains(String.valueOf(DIAS_LIMITE)), anyString());
        }
    }

    @Nested
    @DisplayName("Un aviso por período de inactividad")
    class AvisoUnico {

        @Test
        @DisplayName("Registra el aviso de todo el lote con una sola operación")
        void registraElAviso() {
            PersonaDonante inactivo = donante(30, Email.builder().valor("juan@example.com").build());
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(inactivo), List.of());

            servicio.notificarDonantesInactivos();

            verify(donanteRepository).registrarAvisoDeInactividad(eq(List.of(inactivo.getId())), any());
        }

        @Test
        @DisplayName("Cada lote se confirma en su propia transacción, con sus avisos y su marca de avisado")
        void unaTransaccionPorLote() {
            PersonaDonante primero = donante(30, Email.builder().valor("uno@example.com").build());
            PersonaDonante segundo = donante(30, Email.builder().valor("dos@example.com").build());
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt()))
                    .thenReturn(List.of(primero), List.of(segundo), List.of());

            servicio.notificarDonantesInactivos();

            // Dos lotes con personas y la lectura final que ya no trae a nadie.
            verify(transaccion, times(3)).execute(any());
            verify(donanteRepository).registrarAvisoDeInactividad(eq(List.of(primero.getId())), any());
            verify(donanteRepository).registrarAvisoDeInactividad(eq(List.of(segundo.getId())), any());
        }

        @Test
        @DisplayName("Si la base devuelve otra vez el mismo lote, corta en lugar de repetir avisos")
        void salvaguardaContraRepeticion() {
            PersonaDonante inactivo = donante(30, Email.builder().valor("juan@example.com").build());
            when(donanteRepository.obtenerParaAvisoDeInactividad(any(), anyInt())).thenReturn(List.of(inactivo));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient, times(1)).enviarNotificacion(anyString(), anyString(), anyString());
        }
    }
}
