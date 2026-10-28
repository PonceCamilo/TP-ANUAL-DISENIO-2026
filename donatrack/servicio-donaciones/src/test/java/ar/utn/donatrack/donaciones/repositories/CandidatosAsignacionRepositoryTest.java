package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.repositories.jpa.RankingPrecalculadoJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistencia real de la bandeja de rankings precalculados (Entrega 5).
 *
 * Lo que importa probar contra la base: que el ranking de cada algoritmo vuelva
 * en su orden desde la tabla única de puestos, que la bandeja sobreviva a un
 * clear() del contexto (es decir, que no dependa de la memoria del proceso) y que
 * reemplazarla no deje filas sueltas.
 */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:persistencia_rankings;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CandidatosAsignacionRepository.class)
@DisplayName("CandidatosAsignacionRepository - persistencia real de la bandeja")
class CandidatosAsignacionRepositoryTest {

    private static final String SEMANTICO = "COMPATIBILIDAD_SEMANTICA";
    private static final String SUBATENDIDOS = "PRIORIDAD_SUBATENDIDOS";

    @Autowired private CandidatosAsignacionRepository repositorio;
    @Autowired private RankingPrecalculadoJpaRepository jpa;
    @Autowired private TestEntityManager em;

    private final UUID entidadA = UUID.randomUUID();
    private final UUID entidadB = UUID.randomUUID();
    private final UUID entidadC = UUID.randomUUID();

    private RankingPrecalculado ranking(UUID idDonacion) {
        Map<String, List<ResultadoAsignacion>> rankings = new LinkedHashMap<>();
        rankings.put(SEMANTICO, List.of(new ResultadoAsignacion(entidadA, 3.0), new ResultadoAsignacion(entidadB, 1.0)));
        rankings.put(SUBATENDIDOS, List.of(new ResultadoAsignacion(entidadC, 1.0),
                new ResultadoAsignacion(entidadA, 0.5), new ResultadoAsignacion(entidadB, 0.25)));
        return RankingPrecalculado.de(idDonacion, new ResultadoMatchmaking(rankings), LocalDateTime.of(2026, 10, 26, 3, 30));
    }

    private void sincronizar() {
        jpa.flush();
        em.clear();
    }

    private long filas(String tabla) {
        return ((Number) em.getEntityManager().createNativeQuery("SELECT COUNT(*) FROM " + tabla).getSingleResult())
                .longValue();
    }

    @Test
    @DisplayName("Guarda y relee el ranking de cada algoritmo en su orden, desde una sola tabla")
    void guardaYReleeEnOrden() {
        UUID idDonacion = UUID.randomUUID();
        repositorio.reemplazarTodos(List.of(ranking(idDonacion)));
        sincronizar();

        RankingPrecalculado leido = jpa.findById(idDonacion).orElseThrow();
        ResultadoMatchmaking resultado = leido.resultado(List.of(SEMANTICO, SUBATENDIDOS));

        assertThat(leido.getFechaCalculo()).isEqualTo(LocalDateTime.of(2026, 10, 26, 3, 30));
        assertThat(resultado.rankings().keySet()).containsExactly(SEMANTICO, SUBATENDIDOS);
        assertThat(resultado.ranking(SEMANTICO)).extracting(ResultadoAsignacion::getIdEntidad)
                .containsExactly(entidadA, entidadB);
        assertThat(resultado.ranking(SUBATENDIDOS)).extracting(ResultadoAsignacion::getIdEntidad)
                .containsExactly(entidadC, entidadA, entidadB);
        assertThat(resultado.ranking(SUBATENDIDOS)).extracting(ResultadoAsignacion::getPuntaje)
                .containsExactly(1.0, 0.5, 0.25);
        // Las coincidencias no se guardan: se derivan de los rankings.
        assertThat(resultado.coincidencias()).extracting(ResultadoAsignacion::getIdEntidad)
                .containsExactly(entidadA, entidadB);
        assertThat(filas("ranking_puesto")).isEqualTo(5);
    }

    @Test
    @DisplayName("Devuelve toda la bandeja y elimina el ranking de una donación ya asignada")
    void bandejaYEliminacion() {
        UUID primera = UUID.randomUUID();
        UUID segunda = UUID.randomUUID();
        repositorio.reemplazarTodos(List.of(ranking(primera), ranking(segunda)));
        sincronizar();

        assertThat(repositorio.obtenerTodos()).extracting(RankingPrecalculado::getIdDonacion)
                .containsExactlyInAnyOrder(primera, segunda);

        repositorio.eliminar(primera);
        sincronizar();

        assertThat(repositorio.obtenerTodos()).extracting(RankingPrecalculado::getIdDonacion).containsExactly(segunda);
        assertThat(jpa.findById(primera)).isEmpty();
        assertThat(filas("ranking_puesto")).isEqualTo(5);
    }

    @Test
    @DisplayName("Reemplazar la bandeja borra la corrida anterior entera, sin filas sueltas")
    void reemplazaLaCorridaAnterior() {
        UUID deAyer = UUID.randomUUID();
        UUID deHoy = UUID.randomUUID();
        repositorio.reemplazarTodos(List.of(ranking(deAyer), ranking(UUID.randomUUID())));
        sincronizar();

        // La misma donación puede volver a aparecer en la corrida siguiente.
        repositorio.reemplazarTodos(List.of(ranking(deHoy), ranking(deAyer)));
        sincronizar();

        assertThat(repositorio.obtenerTodos()).extracting(RankingPrecalculado::getIdDonacion)
                .containsExactlyInAnyOrder(deHoy, deAyer);
        assertThat(filas("ranking_puesto")).isEqualTo(10);
    }

    @Test
    @DisplayName("Una corrida sin donaciones deja la bandeja vacía")
    void corridaVacia() {
        repositorio.reemplazarTodos(List.of(ranking(UUID.randomUUID())));
        sincronizar();

        repositorio.reemplazarTodos(List.of());
        sincronizar();

        assertThat(repositorio.obtenerTodos()).isEmpty();
        assertThat(filas("ranking_puesto")).isZero();
        assertThat(filas("ranking_precalculado")).isZero();
    }

    @Test
    @DisplayName("Una donación sin candidatas se guarda igual, sin puestos")
    void sinCandidatas() {
        UUID idDonacion = UUID.randomUUID();
        repositorio.reemplazarTodos(List.of(RankingPrecalculado.de(idDonacion,
                new ResultadoMatchmaking(Map.of(SEMANTICO, List.of())), LocalDateTime.of(2026, 10, 26, 3, 30))));
        sincronizar();

        RankingPrecalculado leido = jpa.findById(idDonacion).orElseThrow();
        assertThat(leido.sinCandidatas()).isTrue();
        assertThat(leido.resultado(List.of(SEMANTICO, SUBATENDIDOS)).huboCoincidencias()).isFalse();
    }

    @Test
    @DisplayName("Un algoritmo sin candidatas no deja puestos, pero al releerlo vuelve como ranking vacío y no inventa coincidencias")
    void algoritmoSinCandidatasNoInventaCoincidencias() {
        UUID idDonacion = UUID.randomUUID();
        Map<String, List<ResultadoAsignacion>> rankings = new LinkedHashMap<>();
        rankings.put(SEMANTICO, List.of());
        rankings.put(SUBATENDIDOS, List.of(new ResultadoAsignacion(entidadA, 1.0), new ResultadoAsignacion(entidadB, 0.5)));
        repositorio.reemplazarTodos(List.of(RankingPrecalculado.de(idDonacion, new ResultadoMatchmaking(rankings),
                LocalDateTime.of(2026, 10, 26, 3, 30))));
        sincronizar();

        ResultadoMatchmaking resultado = jpa.findById(idDonacion).orElseThrow().resultado(List.of(SEMANTICO, SUBATENDIDOS));

        assertThat(resultado.rankings().keySet()).containsExactly(SEMANTICO, SUBATENDIDOS);
        assertThat(resultado.ranking(SEMANTICO)).isEmpty();
        assertThat(resultado.ranking(SUBATENDIDOS)).hasSize(2);
        assertThat(resultado.coincidencias()).isEmpty();
        assertThat(resultado.huboCoincidencias()).isFalse();
    }
}
