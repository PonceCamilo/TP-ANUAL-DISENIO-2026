package ar.utn.donatrack.donaciones.eventos;

import ar.utn.donatrack.donaciones.config.PropiedadesDePrueba;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionAsignada;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionEntregada;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionesRegistradas;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.notificaciones.Notificador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lo que sale hacia afuera cuando algo le pasa a una donación. Antes se probaba
 * dentro de los tests de DonacionService y SegmentadorDonacionesService. Acá el
 * NotificacionClient es un mock: lo que verifica el test es qué avisos se dejan
 * (el envío real lo hace el relay de la bandeja de salida).
 */
@DisplayName("AvisosOyente - avisos a las personas y a incentivos, después de confirmar")
class AvisosOyenteTest {

    private final RepositorioDonaciones donaciones = mock(RepositorioDonaciones.class);
    private final RepositorioPersonasDonantes donantes = mock(RepositorioPersonasDonantes.class);
    private final RepositorioEntidadesBeneficiarias entidades = mock(RepositorioEntidadesBeneficiarias.class);
    private final NotificacionClient notificacionClient = mock(NotificacionClient.class);
    private final IncentivosClient incentivos = mock(IncentivosClient.class);
    private final AvisosOyente oyente = new AvisosOyente(donaciones, donantes, entidades,
            new Notificador(notificacionClient, PropiedadesDePrueba.porDefecto()), incentivos);

    private final UUID idDonante = UUID.randomUUID();
    private final LocalDateTime entrega = LocalDateTime.of(2026, 3, 15, 14, 30);

    private PersonaHumana donanteConEmail(String email) {
        PersonaHumana donante = PersonaHumana.builder().id(idDonante).nombre("Juan").email(email).build();
        if (email != null) {
            donante.definirContactoPredeterminado(Email.builder().valor(email).build());
        }
        return donante;
    }

    private EntidadBeneficiaria entidadConEmail(String email) {
        return EntidadBeneficiaria.builder().id(UUID.randomUUID()).razonSocial("Comedor Los Pibes")
                .contactos(email == null ? new ArrayList<>() : new ArrayList<>(List.of(Email.builder().valor(email).build())))
                .build();
    }

    @Nested
    @DisplayName("Asignación")
    class Asignacion {

        @Test
        @DisplayName("Avisa a la ENTIDAD y al DONANTE por separado")
        void avisaAAmbasPartes() {
            // Requisito del enunciado: las dos partes deben enterarse de la asignación.
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(entidades.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donantes.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));

            oyente.alAsignarse(new DonacionAsignada(UUID.randomUUID(), idDonante, entidad.getId()));

            verify(notificacionClient).enviarNotificacion(eq("comedor@lospibes.org"), anyString(), eq("EMAIL"));
            verify(notificacionClient).enviarNotificacion(eq("juan@example.com"), contains("Comedor Los Pibes"), eq("EMAIL"));
        }

        @Test
        @DisplayName("Si la entidad no tiene contacto, igual se avisa al donante")
        void entidadSinContacto() {
            EntidadBeneficiaria entidad = entidadConEmail(null);
            when(entidades.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donantes.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));

            oyente.alAsignarse(new DonacionAsignada(UUID.randomUUID(), idDonante, entidad.getId()));

            verify(notificacionClient).enviarNotificacion(eq("juan@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("Si el donante no tiene contacto, igual se avisa a la entidad")
        void donanteSinContacto() {
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(entidades.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donantes.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(null)));

            oyente.alAsignarse(new DonacionAsignada(UUID.randomUUID(), idDonante, entidad.getId()));

            verify(notificacionClient).enviarNotificacion(eq("comedor@lospibes.org"), anyString(), anyString());
        }

        @Test
        @DisplayName("Si el aviso no se puede dejar en la bandeja, falla: el cambio se deshace con él")
        void avisoQueFalla() {
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(entidades.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));
            when(donantes.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));
            doThrow(new IllegalStateException("caído")).when(notificacionClient)
                    .enviarNotificacion(anyString(), anyString(), anyString());

            // Corre antes del commit: el aviso y el cambio se confirman juntos o no.
            assertThatThrownBy(() -> oyente.alAsignarse(new DonacionAsignada(UUID.randomUUID(), idDonante, entidad.getId())))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Entrega")
    class Entrega {

        @Test
        @DisplayName("Avisa a incentivos con el contacto del donante y la fecha de entrega")
        void avisaAIncentivos() {
            // Incentivos usa este evento para "donaciones exitosas" y
            // "organizaciones ayudadas", que alimentan las insignias del donante.
            Donacion donacion = new Donacion();
            donacion.setIdDonante(idDonante);
            when(donaciones.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(donantes.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));

            oyente.alEntregarse(new DonacionEntregada(donacion.getId(), idDonante, null, new Subcategoria("arroz"), 5,
                    entrega));

            verify(incentivos).notificarDonacionExitosa(donacion, "juan@example.com", "EMAIL", entrega);
        }

        @Test
        @DisplayName("Si el donante no tiene contacto o ya no existe, no se avisa y no se rompe")
        void sinDonanteOContacto() {
            Donacion donacion = new Donacion();
            when(donaciones.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(donantes.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(null)));

            oyente.alEntregarse(new DonacionEntregada(donacion.getId(), idDonante, null, null, 0, entrega));
            oyente.alEntregarse(new DonacionEntregada(donacion.getId(), null, null, null, 0, entrega));

            verifyNoInteractions(incentivos);
        }
    }

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        @DisplayName("Avisa a incentivos la cantidad de bienes y las categorías de la carga")
        void avisaElRegistro() {
            when(donantes.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));
            LocalDateTime cuando = LocalDateTime.of(2026, 3, 1, 10, 0);

            oyente.alRegistrarse(new DonacionesRegistradas(idDonante, 3, List.of("arroz", "ropa"), cuando));

            verify(incentivos).notificarDonacionRegistrada(idDonante, "juan@example.com", "EMAIL", 3,
                    List.of("arroz", "ropa"), cuando);
        }

        @Test
        @DisplayName("Si el donante no tiene contacto, no se avisa")
        void donanteSinContacto() {
            when(donantes.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail(null)));

            oyente.alRegistrarse(new DonacionesRegistradas(idDonante, 1, List.of("arroz"), LocalDateTime.now()));

            verifyNoInteractions(incentivos);
        }
    }
}
