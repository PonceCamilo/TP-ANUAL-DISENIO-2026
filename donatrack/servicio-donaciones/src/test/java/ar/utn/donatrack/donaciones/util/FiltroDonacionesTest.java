package ar.utn.donatrack.donaciones.util;

import ar.utn.donatrack.donaciones.exceptions.comunes.RangoDeFechasInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FiltroDonaciones")
class FiltroDonacionesTest {

    @Test
    @DisplayName("Los textos en blanco quedan nulos: no filtran")
    void enBlancoEsNulo() {
        FiltroDonaciones filtro = new FiltroDonaciones("  ", null, "", null, false);

        assertThat(filtro.estado()).isNull();
        assertThat(filtro.subcategorias()).isEmpty();
        assertThat(filtro).isEqualTo(new FiltroDonaciones(null, null, null, null, false));
    }

    @Test
    @DisplayName("Los textos con contenido se guardan sin los espacios de los bordes")
    void recortaLosBordes() {
        FiltroDonaciones filtro = new FiltroDonaciones(" EN_DEPOSITO ", null, " arroz ", null, false);

        assertThat(filtro.estado()).isEqualTo("EN_DEPOSITO");
        assertThat(filtro.subcategorias()).containsExactly("arroz");
    }

    @Test
    @DisplayName("Varias subcategorías: descarta las vacías y las repetidas (Entrega 6)")
    void variasSubcategorias() {
        FiltroDonaciones filtro = new FiltroDonaciones(null, null,
                java.util.Arrays.asList(" Arroz ", "", null, "Fideos", "Arroz"), null, false, null, null);

        assertThat(filtro.subcategorias()).containsExactly("Arroz", "Fideos");
    }

    @Test
    @DisplayName("Una lista con una sola subcategoría es el mismo filtro que pasarla sola")
    void unaSolaEsLaMisma() {
        assertThat(new FiltroDonaciones(null, null, List.of("arroz"), null, false, null, null))
                .isEqualTo(new FiltroDonaciones(null, null, "arroz", null, false));
    }

    @Test
    @DisplayName("Sin rango de fechas, desde y hasta quedan nulos: no filtran")
    void sinRangoDeFechas() {
        FiltroDonaciones filtro = new FiltroDonaciones(null, null, null, null, false);

        assertThat(filtro.desde()).isNull();
        assertThat(filtro.hasta()).isNull();
    }

    @Test
    @DisplayName("Un rango de un solo día es válido")
    void rangoDeUnDia() {
        LocalDate dia = LocalDate.of(2026, 11, 10);

        assertThat(new FiltroDonaciones(null, null, null, null, false, dia, dia).desde()).isEqualTo(dia);
    }

    @Test
    @DisplayName("Rechaza un rango con 'desde' posterior a 'hasta'")
    void rangoInvertido() {
        assertThatThrownBy(() -> new FiltroDonaciones(null, null, null, null, false,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9)))
                .isInstanceOf(RangoDeFechasInvalidoException.class)
                .hasMessageContaining("2026-11-10");
    }
}
