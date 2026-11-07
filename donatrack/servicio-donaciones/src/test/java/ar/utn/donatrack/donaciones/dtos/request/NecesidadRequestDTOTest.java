package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CambioTipoNecesidadException;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada DTO de necesidad crea su tipo de necesidad y se aplica sobre una existente. */
@DisplayName("NecesidadRequestDTO - crear() y aplicarEn()")
class NecesidadRequestDTOTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 27);

    @Test
    @DisplayName("El DTO extraordinario crea una extraordinaria con lo recibido en cero")
    void creaExtraordinaria() {
        Necesidad necesidad = NecesidadExtraordinariaRequestDTO.builder()
                .nombre("Colchones").descripcion("De una plaza").subcategoria("Colchones").cantidadObjetivo(50)
                .build().crear(HOY);

        assertThat(necesidad).isInstanceOf(NecesidadExtraordinaria.class);
        assertThat(necesidad.getCantidadObjetivo()).isEqualTo(50);
        assertThat(necesidad.getCantidadRecibida()).isZero();
        assertThat(necesidad.getFechaRegistro()).isEqualTo(HOY);
        assertThat(necesidad.getSubcategoria()).isEqualTo(new Subcategoria("colchones"));
    }

    @Test
    @DisplayName("El DTO recurrente crea una recurrente con su período arrancando ese día")
    void creaRecurrente() {
        Necesidad necesidad = NecesidadRecurrenteRequestDTO.builder()
                .nombre("Fideos").cantidadObjetivo(100).periodo(Periodicidad.SEMANAL)
                .build().crear(HOY);

        assertThat(necesidad).isInstanceOf(NecesidadRecurrente.class);
        NecesidadRecurrente recurrente = (NecesidadRecurrente) necesidad;
        assertThat(recurrente.getPeriodo()).isEqualTo(Periodicidad.SEMANAL);
        assertThat(recurrente.getFechaInicioPeriodo()).isEqualTo(HOY);
        assertThat(recurrente.getSubcategoria()).isNull();
    }

    @Test
    @DisplayName("Una subcategoría en blanco queda sin subcategoría")
    void subcategoriaEnBlanco() {
        Necesidad necesidad = NecesidadExtraordinariaRequestDTO.builder()
                .nombre("Colchones").subcategoria("  ").cantidadObjetivo(5).build().crear(HOY);

        assertThat(necesidad.getSubcategoria()).isNull();
    }

    @Test
    @DisplayName("Aplicar actualiza los datos pero no lo recibido ni el período en curso")
    void aplicaSinTocarLoRecibido() {
        NecesidadRecurrente existente = (NecesidadRecurrente) NecesidadRecurrenteRequestDTO.builder()
                .nombre("Fideos").cantidadObjetivo(100).periodo(Periodicidad.SEMANAL).build().crear(HOY.minusDays(3));
        existente.recibir(40, HOY);

        NecesidadRecurrenteRequestDTO.builder()
                .nombre("Fideos secos").subcategoria("Fideos").cantidadObjetivo(200).periodo(Periodicidad.MENSUAL)
                .build().aplicarEn(existente);

        assertThat(existente.getNombre()).isEqualTo("Fideos secos");
        assertThat(existente.getCantidadObjetivo()).isEqualTo(200);
        assertThat(existente.getPeriodo()).isEqualTo(Periodicidad.MENSUAL);
        assertThat(existente.getSubcategoria()).isEqualTo(new Subcategoria("fideos"));
        assertThat(existente.getCantidadRecibida()).isEqualTo(40);
        assertThat(existente.getFechaInicioPeriodo()).isEqualTo(HOY.minusDays(3));
    }

    @Test
    @DisplayName("No se puede cambiar el tipo de una necesidad")
    void rechazaCambioDeTipo() {
        Necesidad extraordinaria = NecesidadExtraordinariaRequestDTO.builder()
                .nombre("Colchones").cantidadObjetivo(5).build().crear(HOY);

        assertThatThrownBy(() -> NecesidadRecurrenteRequestDTO.builder()
                .nombre("Colchones").cantidadObjetivo(5).periodo(Periodicidad.MENSUAL).build().aplicarEn(extraordinaria))
                .isInstanceOf(CambioTipoNecesidadException.class)
                .hasMessageContaining("EXTRAORDINARIA")
                .hasMessageContaining("RECURRENTE");
    }
}
