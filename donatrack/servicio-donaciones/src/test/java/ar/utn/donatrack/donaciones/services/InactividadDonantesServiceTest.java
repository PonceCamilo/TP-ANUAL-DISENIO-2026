package ar.utn.donatrack.donaciones.services;

import java.time.LocalDateTime;
import org.mockito.ArgumentCaptor;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Whatsapp;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.estado.ActivoState;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    private PersonaDonanteRepositoryInterface donanteRepository;

    @Mock
    private NotificacionClient notificacionClient;

    @InjectMocks
    private InactividadDonantesService servicio;

    /**
     * Donante con la última interacción hace `diasSinInteractuar` días
     * y el medio de contacto predeterminado indicado.
     */
    private PersonaDonante donante(int diasSinInteractuar, MedioDeContacto medioPredeterminado) {
        return PersonaHumana.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email("juan@example.com")
                .estado(new ActivoState())
                .ultimaInteraccion(FechaHoraArgentina.ahora().minusDays(diasSinInteractuar))
                .medioContactoPredeterminado(medioPredeterminado)
                .build();
    }

    @Nested
    @DisplayName("Detección de inactividad (umbral de 20 días)")
    class DeteccionDeInactividad {

        @Test
        @DisplayName("Notifica al donante que lleva más de 20 días sin interactuar")
        void notificaAlInactivo() {
            PersonaDonante inactivo = donante(DIAS_LIMITE + 10, Email.builder().valor("juan@example.com").build());
            when(donanteRepository.obtenerInactivasDesde(any())).thenReturn(List.of(inactivo));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("juan@example.com"), anyString(), eq("EMAIL"));
        }

        @Test
        @DisplayName("Le pide a la base los donantes sin interacción desde hace 20 días")
        void consultaConElUmbralDe20Dias() {
            // El filtro por fecha lo hace la base con el índice de ultima_interaccion
            // (Entrega 5); su semántica se prueba en PersonaDonanteRepositoryTest.
            when(donanteRepository.obtenerInactivasDesde(any())).thenReturn(List.of());
            LocalDateTime antes = FechaHoraArgentina.ahora().minusDays(DIAS_LIMITE);

            servicio.notificarDonantesInactivos();

            ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(donanteRepository).obtenerInactivasDesde(limite.capture());
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
                    .estado(new ActivoState())
                    .medioContactoPredeterminado(Email.builder().valor("nuevo@example.com").build())
                    .build();
            when(donanteRepository.obtenerInactivasDesde(any())).thenReturn(List.of(nuevo));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("nuevo@example.com"), anyString(), eq("EMAIL"));
        }

        @Test
        @DisplayName("Notifica a todos los inactivos que devuelve la base")
        void notificaATodosLosInactivos() {
            PersonaDonante inactivoUno = donante(30, Email.builder().valor("inactivo1@example.com").build());
            PersonaDonante inactivoDos = donante(60, Email.builder().valor("inactivo2@example.com").build());
            when(donanteRepository.obtenerInactivasDesde(any())).thenReturn(List.of(inactivoUno, inactivoDos));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("inactivo1@example.com"), anyString(), anyString());
            verify(notificacionClient).enviarNotificacion(eq("inactivo2@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("Sin donantes cargados la tarea corre sin hacer nada")
        void sinDonantesNoHaceNada() {
            when(donanteRepository.obtenerInactivasDesde(any())).thenReturn(List.of());

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
            when(donanteRepository.obtenerInactivasDesde(any()))
                    .thenReturn(List.of(donante(30, Email.builder().valor("juan@example.com").build())));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("juan@example.com"), anyString(), eq("EMAIL"));
        }

        @Test
        @DisplayName("Un donante con Teléfono predeterminado se notifica por SMS")
        void canalSms() {
            when(donanteRepository.obtenerInactivasDesde(any()))
                    .thenReturn(List.of(donante(30, Telefono.builder().valor("1155667788").build())));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("1155667788"), anyString(), eq("SMS"));
        }

        @Test
        @DisplayName("Un donante con WhatsApp predeterminado se notifica por WHATSAPP")
        void canalWhatsapp() {
            when(donanteRepository.obtenerInactivasDesde(any()))
                    .thenReturn(List.of(donante(30, Whatsapp.builder().valor("1155667788").build())));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("1155667788"), anyString(), eq("WHATSAPP"));
        }

        @Test
        @DisplayName("Un donante inactivo SIN medio predeterminado se omite sin romper la tarea")
        void sinMedioSeOmite() {
            // Importante: la tarea es batch. Un donante mal cargado no puede
            // impedir que se notifique a los demás.
            PersonaDonante sinMedio = donante(30, null);
            PersonaDonante conMedio = donante(30, Email.builder().valor("ok@example.com").build());
            when(donanteRepository.obtenerInactivasDesde(any())).thenReturn(List.of(sinMedio, conMedio));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(eq("ok@example.com"), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Contenido del mensaje")
    class ContenidoDelMensaje {

        @Test
        @DisplayName("El mensaje menciona los días de inactividad e invita a volver a donar")
        void mensajeInvitaADonar() {
            when(donanteRepository.obtenerInactivasDesde(any()))
                    .thenReturn(List.of(donante(30, Email.builder().valor("juan@example.com").build())));

            servicio.notificarDonantesInactivos();

            verify(notificacionClient).enviarNotificacion(
                    anyString(), contains(String.valueOf(DIAS_LIMITE)), anyString());
        }
    }
}
