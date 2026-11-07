package ar.utn.donatrack.donaciones.models.contacto;

import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Medios de contacto: canal polimórfico y predeterminado dentro de la lista")
class ContactoPredeterminadoTest {

    private PersonaHumana persona;

    @BeforeEach
    void crear() {
        persona = PersonaHumana.builder().nombre("Ana").build();
    }

    @Test
    @DisplayName("Cada medio responde su canal: email, SMS para el teléfono y WhatsApp")
    void canalPorTipo() {
        assertThat(Email.builder().valor("a@b.com").build().canal()).isEqualTo(Canal.EMAIL);
        assertThat(Telefono.builder().valor("1").build().canal()).isEqualTo(Canal.SMS);
        assertThat(Whatsapp.builder().valor("1").build().canal()).isEqualTo(Canal.WHATSAPP);
    }

    @Test
    @DisplayName("El predeterminado es uno de los contactos, no una copia aparte")
    void predeterminadoEsUnContacto() {
        Telefono telefono = Telefono.builder().valor("1111").build();

        persona.definirContactoPredeterminado(telefono);

        assertThat(persona.getContactos()).containsExactly(telefono);
        assertThat(persona.getMedioContactoPredeterminado()).isSameAs(telefono);
    }

    @Test
    @DisplayName("Cambiar el contacto del tipo del predeterminado mantiene el canal con el valor nuevo")
    void reemplazarElPredeterminado() {
        persona.definirContactoPredeterminado(Telefono.builder().valor("1111").build());
        persona.reemplazarContacto(Email.builder().valor("ana@mail.com").build());

        persona.reemplazarContacto(Telefono.builder().valor("2222").build());

        assertThat(persona.getMedioContactoPredeterminado().getValor()).isEqualTo("2222");
        assertThat(persona.getContactos()).hasSize(2);
    }

    @Test
    @DisplayName("Reemplazar todos los contactos no pierde el canal predeterminado")
    void reemplazarTodos() {
        persona.definirContactoPredeterminado(Whatsapp.builder().valor("1111").build());

        persona.reemplazarContactos(List.of(Email.builder().valor("ana@mail.com").build()));

        assertThat(persona.getMedioContactoPredeterminado().getValor()).isEqualTo("1111");
        assertThat(persona.getContactos()).hasSize(2);

        persona.reemplazarContactos(List.of(Whatsapp.builder().valor("3333").build()));
        assertThat(persona.getMedioContactoPredeterminado().getValor()).isEqualTo("3333");
    }
}
