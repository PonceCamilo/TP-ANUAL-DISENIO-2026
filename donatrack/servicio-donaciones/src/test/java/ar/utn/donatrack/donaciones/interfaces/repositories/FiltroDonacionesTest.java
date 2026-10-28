package ar.utn.donatrack.donaciones.interfaces.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
