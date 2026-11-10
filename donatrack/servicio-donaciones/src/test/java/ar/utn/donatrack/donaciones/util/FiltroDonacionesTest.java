package ar.utn.donatrack.donaciones.util;

import ar.utn.donatrack.donaciones.exceptions.comunes.RangoDeFechasInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FiltroDonaciones")
class FiltroDonacionesTest {

    @Test
    @DisplayName("Los textos en blanco quedan nulos: no filtran")
    void enBlancoEsNulo() {
        FiltroDonaciones filtro = new FiltroDonaciones("  ", null, "", null, false);

        assertThat(filtro.estado()).isNull();
        assertThat(filtro.subcategoria()).isNull();
        assertThat(filtro).isEqualTo(new FiltroDonaciones(null, null, null, null, false));
    }

    @Test
    @DisplayName("Los textos con contenido se guardan sin los espacios de los bordes")
    void recortaLosBordes() {
        FiltroDonaciones filtro = new FiltroDonaciones(" EN_DEPOSITO ", null, " arroz ", null, false);

        assertThat(filtro.estado()).isEqualTo("EN_DEPOSITO");
        assertThat(filtro.subcategoria()).isEqualTo("arroz");
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
