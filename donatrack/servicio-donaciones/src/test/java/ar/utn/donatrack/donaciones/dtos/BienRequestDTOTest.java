package ar.utn.donatrack.donaciones.dtos;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.models.donacion.bien.TipoBien;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BienRequestDTO: tipo explícito o deducido, sin combinaciones ambiguas")
class BienRequestDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private BienRequestDTO bien(TipoBien tipo, Boolean esNuevo, LocalDate vencimiento) {
        BienRequestDTO dto = new BienRequestDTO();
        dto.setTipo(tipo);
        dto.setSubcategoria("arroz");
        dto.setCantidad(1);
        dto.setUnidad("kg");
        dto.setEsNuevo(esNuevo);
        dto.setFechaVencimiento(vencimiento);
        return dto;
    }

    @Test
    @DisplayName("sin tipo, se deduce como siempre por el dato presente")
    void tipoDeducido() {
        assertThat(bien(null, null, LocalDate.of(2027, 1, 1)).tipoResuelto()).isEqualTo(TipoBien.PERECIBLE);
        assertThat(bien(null, true, null).tipoResuelto()).isEqualTo(TipoBien.CON_ESTADO);
        assertThat(bien(null, null, null).tipoResuelto()).isEqualTo(TipoBien.GENERICO);
    }

    @Test
    @DisplayName("fechaVencimiento y esNuevo a la vez, sin tipo, es inválido (antes se perdía esNuevo)")
    void combinacionAmbiguaEsInvalida() {
        assertThat(validador.validate(bien(null, true, LocalDate.of(2027, 1, 1)))).isNotEmpty();
    }

    @Test
    @DisplayName("un perecedero ya vencido no se registra; vencer hoy sí se permite")
    void vencimientoPasado() {
        LocalDate hoy = ar.utn.donatrack.donaciones.util.FechaHoraArgentina.hoy();
        assertThat(validador.validate(bien(null, null, hoy.minusDays(1)))).isNotEmpty();
        assertThat(validador.validate(bien(null, null, hoy))).isEmpty();
    }

    @Test
    @DisplayName("el tipo explícito exige su dato propio y ninguno ajeno")
    void tipoExplicitoCoherente() {
        assertThat(validador.validate(bien(TipoBien.PERECIBLE, null, LocalDate.of(2027, 1, 1)))).isEmpty();
        assertThat(validador.validate(bien(TipoBien.CON_ESTADO, false, null))).isEmpty();
        assertThat(validador.validate(bien(TipoBien.GENERICO, null, null))).isEmpty();

        assertThat(validador.validate(bien(TipoBien.PERECIBLE, null, null))).isNotEmpty();
        assertThat(validador.validate(bien(TipoBien.CON_ESTADO, true, LocalDate.of(2027, 1, 1)))).isNotEmpty();
        assertThat(validador.validate(bien(TipoBien.GENERICO, true, null))).isNotEmpty();
    }
}
