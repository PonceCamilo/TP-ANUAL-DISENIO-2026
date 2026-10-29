package ar.utn.donatrack.donaciones.config;

import ar.utn.donatrack.donaciones.dtos.request.CampaniaRequestDTO;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las fechas de los pedidos se validan con el día de Argentina: el reloj del
 * validador es el de FechaHoraArgentina, y no el del servidor.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Validación de fechas con el día de Argentina")
class ValidacionConfigTest {

    @Autowired
    private Validator validador;

    @AfterEach
    void restablecerReloj() {
        FechaHoraArgentina.restablecerReloj();
    }

    /** El 1 de enero de 2030 a las 23:30 de Argentina, que en UTC ya es el 2 de enero. */
    private static void alas2330DelPrimeroDeEnero() {
        FechaHoraArgentina.usarReloj(Clock.fixed(Instant.parse("2030-01-02T02:30:00Z"), FechaHoraArgentina.ZONA));
    }

    private Set<ConstraintViolation<CampaniaRequestDTO>> validarCampaniaQueEmpieza(LocalDate inicio) {
        CampaniaRequestDTO campania = CampaniaRequestDTO.builder()
                .descripcionGeneral("Campaña de invierno")
                .fechaInicio(inicio)
                .fechaFin(LocalDate.of(2030, 3, 1))
                .build();
        return validador.validate(campania);
    }

    @Test
    @DisplayName("Una campaña que empieza hoy en Argentina se acepta aunque en UTC ya sea mañana")
    void hoyEnArgentina() {
        alas2330DelPrimeroDeEnero();

        assertThat(validarCampaniaQueEmpieza(LocalDate.of(2030, 1, 1))).isEmpty();
    }

    @Test
    @DisplayName("Un día anterior al de Argentina se rechaza, aunque para el reloj del sistema sea futuro")
    void ayerEnArgentina() {
        alas2330DelPrimeroDeEnero();

        assertThat(validarCampaniaQueEmpieza(LocalDate.of(2029, 12, 31)))
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("La fecha de inicio no puede ser en el pasado");
    }
}
