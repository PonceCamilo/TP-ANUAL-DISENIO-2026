package ar.utn.donatrack.incentivos.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MetricasDonante - calculos del perfil")
class MetricasDonanteTest {

    private final MetricasDonante metricas = new MetricasDonante();

    @Nested
    @DisplayName("Donaciones registradas")
    class DonacionesRegistradas {

        @Test
        @DisplayName("Cuenta solo donaciones registradas como historicas")
        void cuentaDonacionesHistoricas() {
            Donante donante = new Donante();
            donante.registrarDonacion(donacionRegistrada(2, "ALIMENTOS"));
            donante.registrarDonacion(donacionExitosa("Comedor Norte"));

            int total = metricas.totalDonacionesHistoricas(donante);

            assertThat(total).isEqualTo(1);
        }

        @Test
        @DisplayName("Calcula la evolucion por mes y anio")
        void calculaEvolucionPorPeriodo() {
            Donante donante = new Donante();
            donante.registrarDonacion(donacionRegistradaEn(1, "ALIMENTOS", LocalDateTime.of(2026, 5, 2, 10, 0)));
            donante.registrarDonacion(donacionRegistradaEn(1, "ABRIGO", LocalDateTime.of(2026, 5, 15, 10, 0)));
            donante.registrarDonacion(donacionRegistradaEn(1, "HIGIENE", LocalDateTime.of(2026, 6, 2, 10, 0)));

            assertThat(metricas.evolucionPorPeriodo(donante))
                    .extracting(EvolucionPeriodo::getMes, EvolucionPeriodo::getAnio, EvolucionPeriodo::getCantidadDonaciones)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(5, 2026, 2),
                            org.assertj.core.groups.Tuple.tuple(6, 2026, 1)
                    );
        }
    }

    @Nested
    @DisplayName("Progreso de misiones")
    class ProgresoMisiones {

        @Test
        @DisplayName("Cuenta organizaciones ayudadas con donaciones exitosas")
        void cuentaOrganizacionesAyudadas() {
            Donante donante = new Donante();
            donante.registrarDonacion(donacionExitosa("Comedor Norte"));
            donante.registrarDonacion(donacionExitosa("Comedor Norte"));
            donante.registrarDonacion(donacionExitosa("Fundacion Sur"));

            int organizaciones = metricas.organizacionesAyudadas(donante);

            assertThat(organizaciones).isEqualTo(2);
        }

        @Test
        @DisplayName("Toma el record de bienes de una unica donacion")
        void calculaRecordBienes() {
            Donante donante = new Donante();
            donante.registrarDonacion(donacionRegistrada(3, "ALIMENTOS"));
            donante.registrarDonacion(donacionRegistrada(9, "ABRIGO"));

            int record = metricas.recordBienesUnicaDonacion(donante);

            assertThat(record).isEqualTo(9);
        }

        @Test
        @DisplayName("Cuenta categorias distintas sin duplicarlas")
        void cuentaCategoriasDistintas() {
            Donante donante = new Donante();
            donante.registrarDonacion(donacionRegistrada(1, "ALIMENTOS", "ABRIGO"));
            donante.registrarDonacion(donacionRegistrada(1, "ALIMENTOS", "HIGIENE"));

            int categorias = metricas.categoriasDistintasDonadas(donante);

            assertThat(categorias).isEqualTo(3);
        }

        @Test
        @DisplayName("Cuenta las donaciones del mes actual")
        void cuentaDonacionesDelMesActual() {
            Donante donante = new Donante();
            YearMonth actual = YearMonth.now();
            donante.registrarDonacion(donacionRegistradaEn(1, "ALIMENTOS", actual.atDay(5).atTime(12, 0)));
            donante.registrarDonacion(donacionRegistradaEn(1, "ABRIGO", actual.minusMonths(1).atDay(5).atTime(12, 0)));

            int donacionesMesActual = metricas.donacionesMesActual(donante);

            assertThat(donacionesMesActual).isEqualTo(1);
        }
    }

    private DonacionRegistrada donacionRegistrada(int cantidadBienes, String... categorias) {
        return donacionRegistradaEn(cantidadBienes, categorias, LocalDateTime.now());
    }

    private DonacionRegistrada donacionRegistradaEn(int cantidadBienes, String categoria, LocalDateTime fecha) {
        return donacionRegistradaEn(cantidadBienes, new String[]{categoria}, fecha);
    }

    private DonacionRegistrada donacionRegistradaEn(int cantidadBienes, String[] categorias, LocalDateTime fecha) {
        return DonacionRegistrada.builder()
                .fecha(fecha)
                .cantidadBienes(cantidadBienes)
                .categorias(Set.of(categorias))
                .exitosa(false)
                .build();
    }

    private DonacionRegistrada donacionExitosa(String entidadBeneficiaria) {
        return DonacionRegistrada.builder()
                .fecha(LocalDateTime.now())
                .cantidadBienes(1)
                .categorias(Set.of("ALIMENTOS"))
                .entidadBeneficiaria(entidadBeneficiaria)
                .exitosa(true)
                .build();
    }
}
