package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del orquestador de matchmaking.
 *
 * AsignacionDonacionesService corre LOS DOS algoritmos sobre la misma donación
 * y devuelve tres listas: el ranking de cada criterio por separado y la
 * intersección entre ambos. Las entidades que aparecen en las dos listas son
 * las candidatas de mayor confianza, porque los dos criterios las recomiendan.
 *
 * Los algoritmos se mockean a propósito: acá no se prueba CÓMO puntúan
 * (eso está en AlgoritmosAsignacionTest), sino que el servicio los combine bien.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsignacionDonacionesService - orquestación del matchmaking")
class AsignacionDonacionesServiceTest {

    @Mock
    private AlgoritmoCompatibilidadSemantica algoritmoSemantico;

    @Mock
    private AlgoritmoPrioridadSubAtendidos algoritmoSubAtendidos;

    @Mock
    private RepositorioEntidadesBeneficiarias entidadesRepository;

    @InjectMocks
    private AsignacionDonacionesService servicio;

    private Donacion donacion;

    // Ids fijos para poder razonar sobre las intersecciones en cada test.
    private final UUID entidadA = UUID.randomUUID();
    private final UUID entidadB = UUID.randomUUID();
    private final UUID entidadC = UUID.randomUUID();

    @BeforeEach
    void crearDonacion() {
        donacion = new Donacion();
        donacion.setSubcategoria(new Subcategoria("arroz"));
    }

    private ResultadoAsignacion resultado(UUID idEntidad, double puntaje) {
        return new ResultadoAsignacion(idEntidad, puntaje);
    }

    /** Configura ambos algoritmos mock para que devuelvan los rankings indicados. */
    private void configurarRankings(List<ResultadoAsignacion> semantico, List<ResultadoAsignacion> subAtendidos) {
        when(entidadesRepository.buscarTodas()).thenReturn(List.<EntidadBeneficiaria>of());
        when(algoritmoSemantico.evaluar(any(), anyList())).thenReturn(semantico);
        when(algoritmoSubAtendidos.evaluar(any(), anyList())).thenReturn(subAtendidos);
    }

    @Nested
    @DisplayName("generarRanking() - siempre devuelve las tres listas")
    class GenerarRanking {

        @Test
        @DisplayName("Devuelve el ranking de cada algoritmo tal como lo produjo")
        void devuelveAmbosRankings() {
            configurarRankings(
                    List.of(resultado(entidadA, 3.0), resultado(entidadB, 1.0)),
                    List.of(resultado(entidadC, 1.0))
            );

            AsignacionDonacionesService.ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.getRankingSemantico())
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(entidadA, entidadB);
            assertThat(resultado.getRankingSubAtendidos())
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(entidadC);
        }

        @Test
        @DisplayName("Consulta las entidades una sola vez y se las pasa a ambos algoritmos")
        void consultaLasEntidadesUnaVez() {
            // Detalle de eficiencia: no tiene sentido leer el repositorio dos veces
            // para evaluar la misma donación.
            configurarRankings(List.of(), List.of());

            servicio.generarRanking(donacion);

            verify(entidadesRepository).buscarTodas();
            verify(algoritmoSemantico).evaluar(any(), anyList());
            verify(algoritmoSubAtendidos).evaluar(any(), anyList());
        }
    }

    @Nested
    @DisplayName("Cálculo de coincidencias (intersección de los dos rankings)")
    class Coincidencias {

        @Test
        @DisplayName("Las coincidencias son las entidades que aparecen en AMBOS rankings")
        void interseccionDeAmbos() {
            // A y B están en los dos; C solo en el semántico.
            configurarRankings(
                    List.of(resultado(entidadA, 3.0), resultado(entidadC, 2.0), resultado(entidadB, 1.0)),
                    List.of(resultado(entidadB, 0.5), resultado(entidadA, 0.25))
            );

            AsignacionDonacionesService.ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.getCoincidencias())
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactlyInAnyOrder(entidadA, entidadB);
        }

        @Test
        @DisplayName("Las coincidencias conservan el orden y el puntaje del ranking semántico")
        void conservanElOrdenSemantico() {
            // La intersección se arma recorriendo el ranking semántico, así que
            // hereda su orden: la más compatible primero.
            configurarRankings(
                    List.of(resultado(entidadA, 3.0), resultado(entidadB, 1.0)),
                    List.of(resultado(entidadB, 0.5), resultado(entidadA, 0.25))
            );

            AsignacionDonacionesService.ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.getCoincidencias())
                    .extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(entidadA, entidadB);
            assertThat(resultado.getCoincidencias().getFirst().getPuntaje()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Sin entidades en común, las coincidencias quedan vacías pero los rankings se devuelven igual")
        void sinCoincidencias() {
            // Escenario real: los dos criterios apuntan a entidades distintas.
            // El servicio no elige por su cuenta; devuelve todo para que decida
            // la capa superior (y en última instancia, el administrador).
            configurarRankings(
                    List.of(resultado(entidadA, 3.0)),
                    List.of(resultado(entidadB, 1.0))
            );

            AsignacionDonacionesService.ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.getCoincidencias()).isEmpty();
            assertThat(resultado.huboCoincidencias()).isFalse();
            assertThat(resultado.getRankingSemantico()).hasSize(1);
            assertThat(resultado.getRankingSubAtendidos()).hasSize(1);
        }

        @Test
        @DisplayName("huboCoincidencias() es true cuando hay al menos una entidad en común")
        void huboCoincidenciasEsTrue() {
            configurarRankings(
                    List.of(resultado(entidadA, 3.0)),
                    List.of(resultado(entidadA, 1.0))
            );

            assertThat(servicio.generarRanking(donacion).huboCoincidencias()).isTrue();
        }

        @Test
        @DisplayName("Si un algoritmo no devuelve candidatas, no hay coincidencias posibles")
        void unRankingVacioNoDejaCoincidencias() {
            configurarRankings(
                    List.of(resultado(entidadA, 3.0), resultado(entidadB, 1.0)),
                    List.of()
            );

            AsignacionDonacionesService.ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.getCoincidencias()).isEmpty();
            assertThat(resultado.getRankingSemantico()).hasSize(2);
        }

        @Test
        @DisplayName("Sin ninguna entidad cargada, las tres listas vienen vacías (nunca null)")
        void todoVacio() {
            configurarRankings(List.of(), List.of());

            AsignacionDonacionesService.ResultadoMatchmaking resultado = servicio.generarRanking(donacion);

            assertThat(resultado.getCoincidencias()).isEmpty();
            assertThat(resultado.getRankingSemantico()).isEmpty();
            assertThat(resultado.getRankingSubAtendidos()).isEmpty();
        }
    }
}
