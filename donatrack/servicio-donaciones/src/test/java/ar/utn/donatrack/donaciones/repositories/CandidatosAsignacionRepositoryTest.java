package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.repositories.jpa.RankingPrecalculadoJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistencia real de la bandeja de rankings precalculados (Entrega 5).
 *
 * Lo que importa probar contra la base: que las tres listas vuelvan en el orden
 * del ranking, que la bandeja sobreviva a un clear() del contexto (es decir, que
 * ya no dependa de la memoria del proceso) y que vaciarla no deje filas sueltas.
 */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:persistencia_rankings;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CandidatosAsignacionRepository.class)
@DisplayName("CandidatosAsignacionRepository - persistencia real de la bandeja")
class CandidatosAsignacionRepositoryTest {

    @Autowired private CandidatosAsignacionRepository repositorio;
    @Autowired private RankingPrecalculadoJpaRepository jpa;
    @Autowired private TestEntityManager em;

    private final UUID entidadA = UUID.randomUUID();
    private final UUID entidadB = UUID.randomUUID();
    private final UUID entidadC = UUID.randomUUID();

    private RankingPrecalculado ranking(UUID idDonacion) {
        return RankingPrecalculado.builder()
                .idDonacion(idDonacion)
                .coincidencias(List.of(new ResultadoAsignacion(entidadA, 3.0)))
                .rankingSemantico(List.of(new ResultadoAsignacion(entidadA, 3.0), new ResultadoAsignacion(entidadB, 1.0)))
                .rankingSubAtendidos(List.of(new ResultadoAsignacion(entidadC, 1.0),
                        new ResultadoAsignacion(entidadA, 0.5), new ResultadoAsignacion(entidadB, 0.25)))
                .fechaCalculo(LocalDateTime.of(2026, 10, 26, 3, 30))
                .build();
    }

    private void sincronizar() {
        jpa.flush();
        em.clear();
    }

    @Test
    @DisplayName("Guarda y relee las tres listas respetando el orden del ranking")
    void guardaYReleeEnOrden() {
        UUID idDonacion = UUID.randomUUID();
        repositorio.guardar(ranking(idDonacion));
        sincronizar();

        RankingPrecalculado leido = repositorio.obtenerPorDonacion(idDonacion);

        assertThat(leido.getFechaCalculo()).isEqualTo(LocalDateTime.of(2026, 10, 26, 3, 30));
        assertThat(leido.getCoincidencias()).extracting(ResultadoAsignacion::getIdEntidad).containsExactly(entidadA);
        assertThat(leido.getRankingSemantico()).extracting(ResultadoAsignacion::getIdEntidad)
                .containsExactly(entidadA, entidadB);
        assertThat(leido.getRankingSubAtendidos()).extracting(ResultadoAsignacion::getIdEntidad)
                .containsExactly(entidadC, entidadA, entidadB);
        assertThat(leido.getRankingSubAtendidos()).extracting(ResultadoAsignacion::getPuntaje)
                .containsExactly(1.0, 0.5, 0.25);
        assertThat(leido.huboCoincidencias()).isTrue();
    }

    @Test
    @DisplayName("Devuelve toda la bandeja y elimina el ranking de una donación ya asignada")
    void bandejaYEliminacion() {
        UUID primera = UUID.randomUUID();
        UUID segunda = UUID.randomUUID();
        repositorio.guardar(ranking(primera));
        repositorio.guardar(ranking(segunda));
        sincronizar();

        assertThat(repositorio.obtenerTodos()).extracting(RankingPrecalculado::getIdDonacion)
                .containsExactlyInAnyOrder(primera, segunda);

        repositorio.eliminar(primera);
        sincronizar();

        assertThat(repositorio.obtenerTodos()).extracting(RankingPrecalculado::getIdDonacion).containsExactly(segunda);
        assertThat(repositorio.obtenerPorDonacion(primera)).isNull();
    }

    @Test
    @DisplayName("Guardar otro ranking de la misma donación pisa el anterior")
    void pisaElRankingAnterior() {
        UUID idDonacion = UUID.randomUUID();
        repositorio.guardar(ranking(idDonacion));
        sincronizar();

        repositorio.guardar(RankingPrecalculado.builder()
                .idDonacion(idDonacion)
                .coincidencias(List.of())
                .rankingSemantico(List.of())
                .rankingSubAtendidos(List.of(new ResultadoAsignacion(entidadB, 1.0)))
                .fechaCalculo(LocalDateTime.of(2026, 10, 27, 3, 30))
                .build());
        sincronizar();

        RankingPrecalculado leido = repositorio.obtenerPorDonacion(idDonacion);
        assertThat(leido.getRankingSubAtendidos()).extracting(ResultadoAsignacion::getIdEntidad).containsExactly(entidadB);
        assertThat(leido.huboCoincidencias()).isFalse();
    }

    @Test
    @DisplayName("Vaciar la bandeja no deja filas sueltas en las tablas de las listas")
    void limpiarVaciaTodo() {
        repositorio.guardar(ranking(UUID.randomUUID()));
        repositorio.guardar(ranking(UUID.randomUUID()));
        sincronizar();

        repositorio.limpiar();
        sincronizar();

        assertThat(repositorio.obtenerTodos()).isEmpty();
        for (String tabla : List.of("ranking_coincidencia", "ranking_semantico", "ranking_subatendidos")) {
            Number filas = (Number) em.getEntityManager()
                    .createNativeQuery("SELECT COUNT(*) FROM " + tabla).getSingleResult();
            assertThat(filas.longValue()).as(tabla).isZero();
        }
    }
}
