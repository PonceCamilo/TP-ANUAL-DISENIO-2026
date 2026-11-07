package ar.utn.donatrack.incentivos.models;

import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RankingMensual - posiciones del mes")
class RankingMensualTest {

    @Nested
    @DisplayName("Calculo de posiciones")
    class CalculoDePosiciones {

        @Test
        @DisplayName("Ordena primero por misiones completadas")
        void ordenaPorMisionesCompletadas() {
            Donante conMision = donanteConId(UUID.randomUUID());
            Donante sinMision = donanteConId(UUID.randomUUID());
            conMision.agregarInsignia(insigniaObtenidaEn(LocalDateTime.of(2026, 6, 10, 12, 0)));

            RankingMensual ranking = RankingMensual.calcular(List.of(sinMision, conMision), LocalDateTime.of(2026, 6, 24, 12, 0));

            assertThat(ranking.getPosiciones()).extracting(PosicionRanking::getDonante).containsExactly(conMision, sinMision);
        }

        @Test
        @DisplayName("Ante empate usa las donaciones del mes")
        void desempataPorDonacionesDelMes() {
            Donante dosDonaciones = donanteConId(UUID.randomUUID());
            Donante unaDonacion = donanteConId(UUID.randomUUID());
            dosDonaciones.registrarDonacion(donacionRegistradaEn(LocalDateTime.of(2026, 6, 1, 10, 0)));
            dosDonaciones.registrarDonacion(donacionRegistradaEn(LocalDateTime.of(2026, 6, 2, 10, 0)));
            unaDonacion.registrarDonacion(donacionRegistradaEn(LocalDateTime.of(2026, 6, 3, 10, 0)));

            RankingMensual ranking = RankingMensual.calcular(List.of(unaDonacion, dosDonaciones), LocalDateTime.of(2026, 6, 24, 12, 0));

            assertThat(ranking.getPosiciones()).extracting(PosicionRanking::getDonante).containsExactly(dosDonaciones, unaDonacion);
        }

        @Test
        @DisplayName("Guarda el primer dia del mes en cada posicion")
        void guardaPrimerDiaDelMes() {
            Donante donante = donanteConId(UUID.randomUUID());

            RankingMensual ranking = RankingMensual.calcular(List.of(donante), LocalDateTime.of(2026, 6, 24, 18, 30));

            assertThat(ranking.getPeriodoInicio()).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0));
            assertThat(ranking.getPosiciones().get(0).getDiaDeCreacion()).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0));
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("topTres devuelve como maximo tres posiciones")
        void devuelveTopTres() {
            RankingMensual ranking = RankingMensual.calcular(List.of(
                    donanteConId(UUID.randomUUID()),
                    donanteConId(UUID.randomUUID()),
                    donanteConId(UUID.randomUUID()),
                    donanteConId(UUID.randomUUID())
            ), LocalDateTime.of(2026, 6, 24, 12, 0));

            assertThat(ranking.topTres()).hasSize(3);
        }

        @Test
        @DisplayName("posicionDe devuelve el puesto del donante")
        void devuelvePosicionDelDonante() {
            Donante primero = donanteConId(UUID.randomUUID());
            Donante segundo = donanteConId(UUID.randomUUID());
            primero.agregarInsignia(insigniaObtenidaEn(LocalDateTime.of(2026, 6, 10, 12, 0)));

            RankingMensual ranking = RankingMensual.calcular(List.of(segundo, primero), LocalDateTime.of(2026, 6, 24, 12, 0));

            assertThat(ranking.posicionDe(segundo.getId())).isEqualTo(2);
        }
    }

    private Donante donanteConId(UUID id) {
        Donante donante = new Donante();
        donante.setId(id);
        return donante;
    }

    private DonacionRegistrada donacionRegistradaEn(LocalDateTime fecha) {
        return DonacionRegistrada.builder()
                .fecha(fecha)
                .cantidadBienes(1)
                .categorias(Set.of("ALIMENTOS"))
                .exitosa(false)
                .build();
    }

    private InsigniaObtenida insigniaObtenidaEn(LocalDateTime fecha) {
        InsigniaObtenida insignia = new InsigniaObtenida(Insignia.builder().nombre("Semilla").imagen("semilla.png").build(), true);
        insignia.setFechaObtencion(fecha.toLocalDate());
        return insignia;
    }
}
