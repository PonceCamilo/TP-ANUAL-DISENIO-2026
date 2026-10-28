package ar.utn.donatrack.donaciones.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NormalizadorTexto - forma canónica para comparar textos")
class NormalizadorTextoTest {

    @Test
    @DisplayName("Quita bordes, colapsa espacios, saca acentos y pasa a minúsculas")
    void normaliza() {
        assertThat(NormalizadorTexto.normalizar("  Ropa   de  ABRIGO ")).isEqualTo("ropa de abrigo");
        assertThat(NormalizadorTexto.normalizar("Café con Leche")).isEqualTo("cafe con leche");
        assertThat(NormalizadorTexto.normalizar("Pañales")).isEqualTo("panales");
    }

    @Test
    @DisplayName("Las minúsculas no dependen del idioma del servidor (Locale.ROOT)")
    void sinDependerDelLocale() {
        // En turco, "I".toLowerCase() es una i sin punto: con Locale.ROOT no pasa.
        assertThat(NormalizadorTexto.normalizar("ILUMINACION")).isEqualTo("iluminacion");
    }

    @Test
    @DisplayName("null queda como texto vacío")
    void nulo() {
        assertThat(NormalizadorTexto.normalizar(null)).isEmpty();
        assertThat(NormalizadorTexto.normalizar("   ")).isEmpty();
    }
}
