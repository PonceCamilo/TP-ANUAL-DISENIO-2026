package ar.utn.donatrack.incentivos.validations;

import ar.utn.donatrack.incentivos.exceptions.CategoriasDonadasInvalidasException;
import ar.utn.donatrack.incentivos.exceptions.MisionNoEncontradaException;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.misiones.DonacionesExitosas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IncentivosValidator - datos requeridos")
class IncentivosValidatorTest {

    private final IncentivosValidator validator = new IncentivosValidator();

    @Nested
    @DisplayName("Categorias donadas")
    class CategoriasDonadas {

        @Test
        @DisplayName("Acepta categorias cargadas")
        void aceptaCategorias() {
            assertThatCode(() -> validator.validarCategoriasDonadas(List.of("ALIMENTOS", "ABRIGO")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Rechaza una lista vacia")
        void rechazaListaVacia() {
            assertThatThrownBy(() -> validator.validarCategoriasDonadas(List.of()))
                    .isInstanceOf(CategoriasDonadasInvalidasException.class);
        }

        @Test
        @DisplayName("Rechaza una lista nula")
        void rechazaListaNula() {
            assertThatThrownBy(() -> validator.validarCategoriasDonadas(null))
                    .isInstanceOf(CategoriasDonadasInvalidasException.class);
        }

        @Test
        @DisplayName("Rechaza una categoria nula")
        void rechazaCategoriaNula() {
            assertThatThrownBy(() -> validator.validarCategoriasDonadas(java.util.Arrays.asList("ALIMENTOS", null)))
                    .isInstanceOf(CategoriasDonadasInvalidasException.class);
        }

        @Test
        @DisplayName("Rechaza una categoria en blanco")
        void rechazaCategoriaEnBlanco() {
            assertThatThrownBy(() -> validator.validarCategoriasDonadas(List.of("ALIMENTOS", " ")))
                    .isInstanceOf(CategoriasDonadasInvalidasException.class);
        }
    }

    @Nested
    @DisplayName("Misiones disponibles")
    class MisionesDisponibles {

        @Test
        @DisplayName("Acepta una categoria con misiones")
        void aceptaMisiones() {
            assertThatCode(() -> validator.validarMisionesDisponibles(List.of(new DonacionesExitosas(
                    "Primera donacion",
                    "Completar una entrega",
                    new Colaborador(),
                    1,
                    Insignia.builder().nombre("Semilla").imagen("semilla.png").build()
            )), new Colaborador())).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Rechaza una categoria sin misiones")
        void rechazaSinMisiones() {
            assertThatThrownBy(() -> validator.validarMisionesDisponibles(List.of(), new Colaborador()))
                    .isInstanceOf(MisionNoEncontradaException.class);
        }

        @Test
        @DisplayName("Rechaza una lista de misiones nula")
        void rechazaMisionesNulas() {
            assertThatThrownBy(() -> validator.validarMisionesDisponibles(null, new Colaborador()))
                    .isInstanceOf(MisionNoEncontradaException.class);
        }
    }
}
