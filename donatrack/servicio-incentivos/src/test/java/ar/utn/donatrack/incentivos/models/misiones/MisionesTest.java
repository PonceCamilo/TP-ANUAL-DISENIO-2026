package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.DonacionRegistrada;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Misiones - progreso de cada insignia")
class MisionesTest {

    @Nested
    @DisplayName("Donaciones exitosas")
    class DonacionesExitosasTest {

        @Test
        @DisplayName("Se completa cuando la donacion llega a destino")
        void completaConDonacionExitosa() {
            Donante donante = new Donante();
            DonacionesExitosas mision = new DonacionesExitosas("Entrega finalizada", "Completar una entrega", new Colaborador(), 1, insignia());

            donante.registrarDonacion(donacionExitosa("Comedor Norte"));

            assertThat(mision.progresoActual(donante)).isEqualTo(1);
            assertThat(mision.estaCompletada(donante)).isTrue();
            assertThat(mision.restante(donante)).isZero();
        }

        @Test
        @DisplayName("Una donacion registrada todavia no completa la mision")
        void ignoraDonacionRegistrada() {
            Donante donante = new Donante();
            DonacionesExitosas mision = new DonacionesExitosas("Entrega finalizada", "Completar una entrega", new Colaborador(), 1, insignia());

            donante.registrarDonacion(donacionRegistrada(2, "ALIMENTOS"));

            assertThat(mision.progresoActual(donante)).isZero();
            assertThat(mision.estaCompletada(donante)).isFalse();
        }
    }

    @Nested
    @DisplayName("Racha")
    class RachaTest {

        @Test
        @DisplayName("Cuenta los meses consecutivos con donaciones registradas")
        void cuentaMesesConsecutivos() {
            Donante donante = new Donante();
            LocalDate fechaReferencia = LocalDate.of(2026, 6, 24);
            donante.registrarDonacion(donacionRegistradaEn(1, "ALIMENTOS", LocalDateTime.of(2026, 6, 5, 12, 0)));
            donante.registrarDonacion(donacionRegistradaEn(1, "ABRIGO", LocalDateTime.of(2026, 5, 5, 12, 0)));
            donante.registrarDonacion(donacionRegistradaEn(1, "HIGIENE", LocalDateTime.of(2026, 4, 5, 12, 0)));

            int progreso = new ProgresoRacha().mesesConsecutivosDonando(donante, fechaReferencia);

            assertThat(progreso).isEqualTo(3);
        }

        @Test
        @DisplayName("Pierde la racha si pasa un mes completo sin donar")
        void pierdeRachaSiPasaUnMesCompleto() {
            Donante donante = new Donante();
            donante.registrarDonacion(donacionRegistradaEn(1, "ALIMENTOS", LocalDateTime.of(2026, 4, 10, 12, 0)));

            boolean perdio = new ProgresoRacha().pasoUnMesCompletoSinDonaciones(donante, LocalDate.of(2026, 6, 24));

            assertThat(perdio).isTrue();
        }

        @Test
        @DisplayName("No pierde la racha antes de tener donaciones")
        void noPierdeRachaSinHistorial() {
            Donante donante = new Donante();

            boolean perdio = new ProgresoRacha().pasoUnMesCompletoSinDonaciones(donante, LocalDate.of(2026, 6, 24));

            assertThat(perdio).isFalse();
        }
    }

    @Nested
    @DisplayName("Habil donador")
    class HabilDonadorTest {

        @Test
        @DisplayName("Se completa con una donacion que alcanza la cantidad de bienes")
        void completaConCantidadDeBienes() {
            Donante donante = new Donante();
            HabilDonador mision = new HabilDonador("Gran donacion", "Donar muchos bienes", new Colaborador(), 10, insignia());

            donante.registrarDonacion(donacionRegistrada(12, "ALIMENTOS"));

            assertThat(mision.progresoActual(donante)).isEqualTo(12);
            assertThat(mision.estaCompletada(donante)).isTrue();
        }
    }

    @Nested
    @DisplayName("Completitud")
    class CompletitudTest {

        @Test
        @DisplayName("Se completa al donar categorias distintas")
        void completaConCategoriasDistintas() {
            Donante donante = new Donante();
            Completitud mision = new Completitud("Ayuda integral", "Donar varias categorias", new Colaborador(), 3, insignia());

            donante.registrarDonacion(donacionRegistrada(1, "ALIMENTOS", "ABRIGO"));
            donante.registrarDonacion(donacionRegistrada(1, "HIGIENE"));

            assertThat(mision.progresoActual(donante)).isEqualTo(3);
            assertThat(mision.estaCompletada(donante)).isTrue();
        }
    }

    @Test
    @DisplayName("Otorgar una insignia crea una insignia visible")
    void otorgaInsigniaVisible() {
        DonacionesExitosas mision = new DonacionesExitosas("Entrega finalizada", "Completar una entrega", new Colaborador(), 1, insignia());

        InsigniaObtenida obtenida = mision.otorgarInsignia();

        assertThat(obtenida.getInsignia().getNombre()).isEqualTo("Semilla");
        assertThat(obtenida.isVisibilidad()).isTrue();
        assertThat(obtenida.getFechaObtencion()).isEqualTo(LocalDate.now());
    }

    private Insignia insignia() {
        return Insignia.builder().nombre("Semilla").imagen("semilla.png").build();
    }

    private DonacionRegistrada donacionRegistrada(int cantidadBienes, String... categorias) {
        return donacionRegistradaEn(cantidadBienes, categorias, LocalDateTime.now());
    }

    private DonacionRegistrada donacionRegistradaEn(int cantidadBienes, String[] categorias, LocalDateTime fecha) {
        return DonacionRegistrada.builder()
                .fecha(fecha)
                .cantidadBienes(cantidadBienes)
                .categorias(Set.of(categorias))
                .exitosa(false)
                .build();
    }

    private DonacionRegistrada donacionRegistradaEn(int cantidadBienes, String categoria, LocalDateTime fecha) {
        return donacionRegistradaEn(cantidadBienes, new String[]{categoria}, fecha);
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
