package ar.utn.donatrack.donaciones.notificaciones;

import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.config.DonacionesProperties;
import ar.utn.donatrack.donaciones.models.contacto.Canal;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Whatsapp;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Notificador: a quién y por qué canal se avisa")
class NotificadorTest {

    private final NotificacionClient cliente = mock(NotificacionClient.class);
    private final Notificador notificador = new Notificador(cliente, DonacionesProperties.porDefecto());

    private PersonaHumana donanteCon(Canal canal) {
        PersonaHumana persona = PersonaHumana.builder().id(UUID.randomUUID()).email("ana@mail.com").build();
        switch (canal) {
            case EMAIL -> persona.definirContactoPredeterminado(Email.builder().valor("otra@mail.com").build());
            case SMS -> persona.definirContactoPredeterminado(Telefono.builder().valor("1111").build());
            case WHATSAPP -> persona.definirContactoPredeterminado(Whatsapp.builder().valor("2222").build());
        }
        return persona;
    }

    @Test
    @DisplayName("A la persona donante se le escribe por su canal predeterminado")
    void canalPredeterminado() {
        assertThat(notificador.destinoDe(donanteCon(Canal.SMS)))
                .contains(new Notificador.Destino("1111", Canal.SMS));
        assertThat(notificador.destinoDe(donanteCon(Canal.WHATSAPP)))
                .contains(new Notificador.Destino("2222", Canal.WHATSAPP));
        assertThat(notificador.destinoDe(donanteCon(Canal.EMAIL)))
                .contains(new Notificador.Destino("otra@mail.com", Canal.EMAIL));
    }

    @Test
    @DisplayName("Sin predeterminado, se usa el email de la persona; sin email, no hay destino")
    void respaldoEmail() {
        assertThat(notificador.destinoDe(PersonaHumana.builder().email("ana@mail.com").build()))
                .contains(new Notificador.Destino("ana@mail.com", Canal.EMAIL));
        assertThat(notificador.destinoDe(PersonaHumana.builder().build())).isEmpty();
    }

    @Test
    @DisplayName("A la entidad se le escribe a su email; si no tiene, a su primer contacto")
    void destinoDeEntidad() {
        EntidadBeneficiaria conEmail = EntidadBeneficiaria.builder()
                .contactos(List.of(Telefono.builder().valor("1111").build(), Email.builder().valor("e@mail.com").build()))
                .build();
        EntidadBeneficiaria sinEmail = EntidadBeneficiaria.builder()
                .contactos(List.of(Whatsapp.builder().valor("3333").build()))
                .build();

        assertThat(notificador.destinoDe(conEmail)).contains(new Notificador.Destino("e@mail.com", Canal.EMAIL));
        assertThat(notificador.destinoDe(sinEmail)).contains(new Notificador.Destino("3333", Canal.WHATSAPP));
    }

    @Test
    @DisplayName("La asignación se le avisa a la persona donante por su canal (antes siempre era EMAIL)")
    void asignacionPorElCanalDeLaPersona() {
        EntidadBeneficiaria entidad = EntidadBeneficiaria.builder().razonSocial("Comedor")
                .contactos(List.of(Email.builder().valor("e@mail.com").build())).build();

        notificador.donacionAsignada(donanteCon(Canal.WHATSAPP), entidad);

        verify(cliente).enviarNotificacion(eq("2222"), contains("Comedor"), eq("WHATSAPP"));
        verify(cliente).enviarNotificacion(eq("e@mail.com"), anyString(), eq("EMAIL"));
    }

    @Test
    @DisplayName("La entrega fallida avisa además al email de administración configurado")
    void entregaFallidaAvisaAlAdministrador() {
        notificador.entregaFallida(UUID.randomUUID(), null, null, "Nadie en el domicilio", true);

        verify(cliente).enviarNotificacion(eq("admin@donatrack.org"), contains("Nadie en el domicilio"), eq("EMAIL"));
    }
}
