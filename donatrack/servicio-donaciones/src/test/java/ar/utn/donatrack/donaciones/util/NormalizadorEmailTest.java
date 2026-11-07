package ar.utn.donatrack.donaciones.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NormalizadorEmail.esValido - forma de un email")
class NormalizadorEmailTest {

    @ParameterizedTest(name = "{0} es válido")
    @ValueSource(strings = {"ana@mail.com", "ana.perez+donaciones@mail.com.ar", " ana@mail.com ",
            "benjamíncastro8929@yahoo.com", "a@b.co"})
    void validos(String email) {
        assertThat(NormalizadorEmail.esValido(email)).isTrue();
    }

    @ParameterizedTest(name = "{0} no es válido")
    @ValueSource(strings = {"", "sin-arroba", "@mail.com", "ana@", "ana@@mail.com", "ana@mail", "ana@mail.c",
            "ana perez@mail.com", ".ana@mail.com", "ana.@mail.com", "ana..perez@mail.com", "ana@-mail.com",
            "ana@mail-.com", "ana@mail..com"})
    void invalidos(String email) {
        assertThat(NormalizadorEmail.esValido(email)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    void nulo(String email) {
        assertThat(NormalizadorEmail.esValido(email)).isFalse();
    }
}
