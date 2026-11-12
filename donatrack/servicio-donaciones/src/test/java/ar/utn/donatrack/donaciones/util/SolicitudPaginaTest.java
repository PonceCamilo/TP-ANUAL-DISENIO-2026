package ar.utn.donatrack.donaciones.util;

import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.comunes.PaginaInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SolicitudPagina: la API numera desde 1, por dentro se usa el índice desde 0")
class SolicitudPaginaTest {

    @Test
    @DisplayName("Sin parámetros pide la primera página (índice 0) con el tamaño por defecto")
    void valoresPorDefecto() {
        assertThat(SolicitudPagina.de(null, null))
                .isEqualTo(new SolicitudPagina(0, SolicitudPagina.TAMANIO_POR_DEFECTO));
    }

    @Test
    @DisplayName("La página 1 de la API es el índice 0, y la 3 es el índice 2")
    void conviertePaginaEnIndice() {
        assertThat(SolicitudPagina.de(1, 10).numero()).isZero();
        assertThat(SolicitudPagina.de(3, 10).numero()).isEqualTo(2);
    }

    @Test
    @DisplayName("La página 0 y las negativas son un error de quien llama")
    void rechazaPaginasMenoresAUno() {
        assertThatThrownBy(() -> SolicitudPagina.de(0, 10))
                .isInstanceOf(PaginaInvalidaException.class)
                .hasMessageContaining("empiezan en 1");
        assertThatThrownBy(() -> SolicitudPagina.de(-1, 10))
                .isInstanceOf(PaginaInvalidaException.class);
    }

    @Test
    @DisplayName("El tamaño sigue limitado entre 1 y 100")
    void limitaElTamanio() {
        assertThatThrownBy(() -> SolicitudPagina.de(1, 0)).isInstanceOf(PaginaInvalidaException.class);
        assertThatThrownBy(() -> SolicitudPagina.de(1, SolicitudPagina.TAMANIO_MAXIMO + 1))
                .isInstanceOf(PaginaInvalidaException.class);
    }

    @Test
    @DisplayName("La respuesta vuelve a numerar desde 1: el índice 0 sale como página 1")
    void laRespuestaNumeraDesdeUno() {
        Pagina<String> primera = new Pagina<>(List.of("a"), 0, 20, 1);
        Pagina<String> tercera = new Pagina<>(List.of(), 2, 20, 41);

        assertThat(PaginaResponseDTO.desde(primera).getPagina()).isEqualTo(1);
        assertThat(PaginaResponseDTO.desde(tercera).getPagina()).isEqualTo(3);
        assertThat(PaginaResponseDTO.desde(tercera).getTotalPaginas()).isEqualTo(3);
    }
}
