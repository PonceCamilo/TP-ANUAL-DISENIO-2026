package ar.utn.donatrack.donaciones.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NormalizadorEmail - forma canónica del email")
class NormalizadorEmailTest {

    @Test
    @DisplayName("Pasa a minúsculas y quita los espacios de los extremos")
    void minusculasYSinEspacios() {
        assertThat(NormalizadorEmail.normalizar("  Ana.Perez@Mail.COM ")).isEqualTo("ana.perez@mail.com");
    }

    @Test
    @DisplayName("Una tilde escrita con carácter combinado queda igual que la precompuesta")
    void unificaLaFormaDeLasTildes() {
        // "i" + acento combinado (U+0301) frente a "í" (U+00ED): se ven iguales,
        // pero sin normalizar la base los consideraría emails distintos.
        String combinado = "benjamín@mail.com";
        String precompuesto = "benjamín@mail.com";

        assertThat(NormalizadorEmail.normalizar(combinado)).isEqualTo(precompuesto);
    }

    @Test
    @DisplayName("Las mayúsculas acentuadas también pasan a minúsculas")
    void mayusculasAcentuadas() {
        assertThat(NormalizadorEmail.normalizar("SOFÍA@MAIL.COM")).isEqualTo("sofía@mail.com");
    }

    @Test
    @DisplayName("Un email nulo sigue siendo nulo")
    void nulo() {
        assertThat(NormalizadorEmail.normalizar(null)).isNull();
    }
}
