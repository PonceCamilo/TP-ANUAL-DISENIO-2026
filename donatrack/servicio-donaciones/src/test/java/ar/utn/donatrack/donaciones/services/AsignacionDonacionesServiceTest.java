package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.config.PropiedadesDePrueba;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.services.AlgoritmoAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del orquestador de matchmaking.
 *
 * AsignacionDonacionesService corre TODOS los algoritmos registrados sobre la
 * misma donación y devuelve el ranking de cada uno por su nombre. Las entidades
 * que aparecen en todos los rankings (las coincidencias) son las candidatas de
 * mayor confianza.
 *
 * Los algoritmos se mockean a propósito: acá no se prueba CÓMO puntúan (eso está
 * en AlgoritmosAsignacionTest), sino que el servicio los combine bien.
 */
@DisplayName("AsignacionDonacionesService - orquestación del matchmaking")
class AsignacionDonacionesServiceTest {

    private static final String SEMANTICO = AlgoritmoCompatibilidadSemantica.NOMBRE;
    private static final String SUBATENDIDOS = AlgoritmoPrioridadSubAtendidos.NOMBRE;

    private final AlgoritmoAsignacion algoritmoSemantico = algoritmo(SEMANTICO);
    private final AlgoritmoAsignacion algoritmoSubAtendidos = algoritmo(SUBATENDIDOS);
    private final RepositorioEntidadesBeneficiarias entidadesRepository = mock(RepositorioEntidadesBeneficiarias.class);

    private AsignacionDonacionesService servicio;
    private Donacion donacion;

    // Ids fijos para poder razonar sobre las intersecciones en cada test.
    private final UUID entidadA = UUID.randomUUID();
    private final UUID entidadB = UUID.randomUUID();
    private final UUID entidadC = UUID.randomUUID();

    private static AlgoritmoAsignacion algoritmo(String nombre) {
        AlgoritmoAsignacion algoritmo = mock(AlgoritmoAsignacion.class);
        when(algoritmo.nombre()).thenReturn(nombre);
        return algoritmo;
    }

    @BeforeEach
    void crearServicio() {
        servicio = new AsignacionDonacionesService(List.of(algoritmoSemantico, algoritmoSubAtendidos),
                entidadesRepository, PropiedadesDePrueba.porDefecto());
        donacion = new Donacion();
        donacion.setSubcategoria(new Subcategoria("arroz"));
    }

    private ResultadoAsignacion resultado(UUID idEntidad, double puntaje) {
        return new ResultadoAsignacion(idEntidad, puntaje);
    }

    /** Configura los dos algoritmos mock para que devuelvan los rankings indicados. */
    private void configurarRankings(List<ResultadoAsignacion> semantico, List<ResultadoAsignacion> subAtendidos) {
        when(entidadesRepository.buscarTodas()).thenReturn(List.<EntidadBeneficiaria>of());
        when(algoritmoSemantico.evaluar(any(), anyList(), anyInt())).thenReturn(semantico);
        when(algoritmoSubAtendidos.evaluar(any(), anyList(), anyInt())).thenReturn(subAtendidos);
    }

    @Nested
    @DisplayName("generarRanking() - un ranking por algoritmo")
    class GenerarRanking {

        @Test
        @DisplayName("Devuelve el ranking de cada algoritmo, por nombre y en el orden de ejecución")
        void devuelveLosRankingsPorNombre() {
            configurarRankings(
                    List.of(resultado(entidadA, 3.0), resultado(entidadB, 1.0)),
                    List.of(resultado(entidadC, 1.0)));

            ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.rankings().keySet()).containsExactly(SEMANTICO, SUBATENDIDOS);
            assertThat(resultado.ranking(SEMANTICO)).extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(entidadA, entidadB);
            assertThat(resultado.ranking(SUBATENDIDOS)).extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(entidadC);
        }

        @Test
        @DisplayName("Consulta las entidades una sola vez y pide a cada algoritmo el tamaño configurado")
        void consultaLasEntidadesUnaVez() {
            // No tiene sentido leer el repositorio una vez por algoritmo para evaluar
            // la misma donación. El tamaño del ranking sale de la configuración (10).
            configurarRankings(List.of(), List.of());

            servicio.generarRanking(donacion);

            verify(entidadesRepository).buscarTodas();
            verify(algoritmoSemantico).evaluar(any(), anyList(), eq(10));
            verify(algoritmoSubAtendidos).evaluar(any(), anyList(), eq(10));
        }

        @Test
        @DisplayName("Un algoritmo nuevo se enchufa sin tocar el orquestador: aparece con su nombre")
        void algoritmoNuevo() {
            AlgoritmoAsignacion cercania = algoritmo("CERCANIA");
            when(cercania.evaluar(any(), anyList(), anyInt())).thenReturn(List.of(resultado(entidadA, 0.9)));
            when(algoritmoSemantico.evaluar(any(), anyList(), anyInt())).thenReturn(List.of(resultado(entidadA, 3.0)));
            when(algoritmoSubAtendidos.evaluar(any(), anyList(), anyInt())).thenReturn(List.of(resultado(entidadA, 1.0)));
            AsignacionDonacionesService conTres = new AsignacionDonacionesService(
                    List.of(algoritmoSemantico, algoritmoSubAtendidos, cercania), entidadesRepository,
                    PropiedadesDePrueba.porDefecto());

            ResultadoMatchmaking resultado = conTres.generarRanking(donacion, List.of());

            assertThat(resultado.rankings().keySet()).containsExactly(SEMANTICO, SUBATENDIDOS, "CERCANIA");
            assertThat(resultado.coincidencias()).extracting(ResultadoAsignacion::getIdEntidad).containsExactly(entidadA);
        }

        @Test
        @DisplayName("Dos algoritmos con el mismo nombre se rechazan al armar el servicio")
        void nombresRepetidos() {
            assertThatThrownBy(() -> new AsignacionDonacionesService(
                    List.of(algoritmoSemantico, algoritmo(SEMANTICO)), entidadesRepository,
                    PropiedadesDePrueba.porDefecto()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(SEMANTICO);
        }
    }

    @Nested
    @DisplayName("Cálculo de coincidencias (intersección de los rankings)")
    class Coincidencias {

        @Test
        @DisplayName("Las coincidencias son las entidades que aparecen en TODOS los rankings")
        void interseccionDeTodos() {
            // A y B están en los dos; C solo en el semántico.
            configurarRankings(
                    List.of(resultado(entidadA, 3.0), resultado(entidadC, 2.0), resultado(entidadB, 1.0)),
                    List.of(resultado(entidadB, 0.5), resultado(entidadA, 0.25)));

            ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.coincidencias())
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactlyInAnyOrder(entidadA, entidadB);
        }

        @Test
        @DisplayName("Las coincidencias conservan el orden y el puntaje del primer ranking")
        void conservanElOrdenDelPrimero() {
            // La intersección se arma recorriendo el primer ranking (el semántico),
            // así que hereda su orden: la más compatible primero.
            configurarRankings(
                    List.of(resultado(entidadA, 3.0), resultado(entidadB, 1.0)),
                    List.of(resultado(entidadB, 0.5), resultado(entidadA, 0.25)));

            ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.coincidencias())
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(entidadA, entidadB);
            assertThat(resultado.coincidencias().getFirst().getPuntaje()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Sin entidades en común, las coincidencias quedan vacías pero los rankings se devuelven igual")
        void sinCoincidencias() {
            // Escenario real: los criterios apuntan a entidades distintas. El
            // servicio no elige por su cuenta; devuelve todo para que decida la
            // capa superior (y en última instancia, el administrador).
            configurarRankings(List.of(resultado(entidadA, 3.0)), List.of(resultado(entidadB, 1.0)));

            ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.coincidencias()).isEmpty();
            assertThat(resultado.huboCoincidencias()).isFalse();
            assertThat(resultado.ranking(SEMANTICO)).hasSize(1);
            assertThat(resultado.ranking(SUBATENDIDOS)).hasSize(1);
        }

        @Test
        @DisplayName("huboCoincidencias() es true cuando hay al menos una entidad en común")
        void huboCoincidenciasEsTrue() {
            configurarRankings(List.of(resultado(entidadA, 3.0)), List.of(resultado(entidadA, 1.0)));

            assertThat(servicio.generarRanking(donacion).huboCoincidencias()).isTrue();
        }

        @Test
        @DisplayName("Si un algoritmo no devuelve candidatas, no hay coincidencias posibles")
        void unRankingVacioNoDejaCoincidencias() {
            configurarRankings(List.of(resultado(entidadA, 3.0), resultado(entidadB, 1.0)), List.of());

            ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.coincidencias()).isEmpty();
            assertThat(resultado.ranking(SEMANTICO)).hasSize(2);
        }

        @Test
        @DisplayName("Sin ninguna entidad cargada, los rankings vienen vacíos (nunca null)")
        void todoVacio() {
            configurarRankings(List.of(), List.of());

            ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.coincidencias()).isEmpty();
            assertThat(resultado.sinCandidatas()).isTrue();
            assertThat(resultado.ranking(SEMANTICO)).isEmpty();
            assertThat(resultado.ranking("NO_EXISTE")).isEmpty();
        }
    }
}
