package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.CandidatosAsignacionRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del proceso nocturno de matchmaking.
 *
 * El enunciado (Entrega 2) pide ejecutar los algoritmos de asignación sobre
 * TODAS las donaciones que están EN_DEPOSITO, y hacerlo "en horarios de baja
 * carga" para no degradar el desempeño. Este servicio es esa ejecución.
 *
 * Decisión de diseño que estos tests fijan: el proceso NO asigna por su cuenta.
 * Calcula y guarda los rankings; la asignación efectiva sigue siendo una acción
 * explícita de una persona administradora (DonacionService.asignar).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsignacionBatchService - matchmaking nocturno de las donaciones en depósito")
class AsignacionBatchServiceTest {

    @Mock
    private DonacionesRepositoryInterface donacionesRepositorio;

    @Mock
    private AsignacionDonacionesService asignacionService;

    @Mock
    private CandidatosAsignacionRepositoryInterface candidatosRepositorio;

    @InjectMocks
    private AsignacionBatchService servicio;

    @Captor
    private ArgumentCaptor<RankingPrecalculado> rankingCaptor;

    /** Donación EN_DEPOSITO de la subcategoría indicada. */
    private Donacion donacionEnDeposito(String subcategoria) {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(UUID.randomUUID());
        donacion.setSubcategoria(new Subcategoria(subcategoria));
        return donacion;
    }

    /** Resultado de matchmaking con las tres listas indicadas. */
    private AsignacionDonacionesService.ResultadoMatchmaking matchmaking(
            List<ResultadoAsignacion> coincidencias,
            List<ResultadoAsignacion> semantico,
            List<ResultadoAsignacion> subAtendidos) {
        return new AsignacionDonacionesService.ResultadoMatchmaking(coincidencias, semantico, subAtendidos);
    }

    private ResultadoAsignacion resultado(double puntaje) {
        return new ResultadoAsignacion(UUID.randomUUID(), puntaje);
    }

    @Nested
    @DisplayName("Alcance del proceso")
    class Alcance {

        @Test
        @DisplayName("Solo procesa las donaciones que están EN_DEPOSITO")
        void soloLasDeDeposito() {
            // Una donación ya asignada o entregada no vuelve al matchmaking:
            // se consulta al repositorio filtrando por estado, no todas.
            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO")).thenReturn(List.of());

            servicio.ejecutarMatchmakingNocturno();

            verify(donacionesRepositorio).obtenerPorEstado("EN_DEPOSITO");
            verify(donacionesRepositorio, never()).obtenerTodas();
        }

        @Test
        @DisplayName("Calcula y guarda un ranking por cada donación en depósito")
        void unRankingPorDonacion() {
            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO"))
                    .thenReturn(List.of(donacionEnDeposito("arroz"), donacionEnDeposito("ropa")));
            when(asignacionService.generarRanking(any()))
                    .thenReturn(matchmaking(List.of(), List.of(resultado(1.0)), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            verify(asignacionService, times(2)).generarRanking(any());
            verify(candidatosRepositorio, times(2)).guardar(any());
        }

        @Test
        @DisplayName("Sin donaciones en depósito no hace nada y no falla")
        void sinDonaciones() {
            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO")).thenReturn(List.of());

            assertThatCode(() -> servicio.ejecutarMatchmakingNocturno()).doesNotThrowAnyException();

            verify(candidatosRepositorio, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("Contenido del ranking guardado")
    class ContenidoDelRanking {

        @Test
        @DisplayName("Guarda las tres listas del matchmaking junto al id de la donación y la fecha")
        void guardaLasTresListas() {
            Donacion donacion = donacionEnDeposito("arroz");
            ResultadoAsignacion coincidencia = resultado(3.0);

            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO")).thenReturn(List.of(donacion));
            when(asignacionService.generarRanking(donacion)).thenReturn(matchmaking(
                    List.of(coincidencia),
                    List.of(coincidencia, resultado(1.0)),
                    List.of(coincidencia)));

            servicio.ejecutarMatchmakingNocturno();

            verify(candidatosRepositorio).guardar(rankingCaptor.capture());
            RankingPrecalculado guardado = rankingCaptor.getValue();

            assertThat(guardado.getIdDonacion()).isEqualTo(donacion.getId());
            assertThat(guardado.getCoincidencias()).hasSize(1);
            assertThat(guardado.getRankingSemantico()).hasSize(2);
            assertThat(guardado.getRankingSubAtendidos()).hasSize(1);
            assertThat(guardado.getFechaCalculo()).isNotNull();
        }

        @Test
        @DisplayName("huboCoincidencias() es true cuando ambos algoritmos recomiendan la misma entidad")
        void detectaCoincidencias() {
            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO"))
                    .thenReturn(List.of(donacionEnDeposito("arroz")));
            when(asignacionService.generarRanking(any()))
                    .thenReturn(matchmaking(List.of(resultado(3.0)), List.of(resultado(3.0)), List.of(resultado(1.0))));

            servicio.ejecutarMatchmakingNocturno();

            verify(candidatosRepositorio).guardar(rankingCaptor.capture());
            assertThat(rankingCaptor.getValue().huboCoincidencias()).isTrue();
        }

        @Test
        @DisplayName("sinCandidatas() es true cuando ningún algoritmo devolvió entidades")
        void detectaDonacionSinCandidatas() {
            // Pasa cuando ninguna entidad declaró una necesidad compatible: la
            // donación se queda en depósito y el administrador lo ve en la bandeja.
            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO"))
                    .thenReturn(List.of(donacionEnDeposito("bicicletas")));
            when(asignacionService.generarRanking(any()))
                    .thenReturn(matchmaking(List.of(), List.of(), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            verify(candidatosRepositorio).guardar(rankingCaptor.capture());
            assertThat(rankingCaptor.getValue().sinCandidatas()).isTrue();
        }
    }

    @Nested
    @DisplayName("Vigencia de los resultados")
    class Vigencia {

        @Test
        @DisplayName("Descarta los rankings de la corrida anterior ANTES de guardar los nuevos")
        void limpiaAntesDeGuardar() {
            // Si no se limpiara, quedarían recomendaciones de donaciones que ya
            // fueron asignadas o que se calcularon con necesidades viejas.
            Donacion donacion = donacionEnDeposito("arroz");
            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO")).thenReturn(List.of(donacion));
            when(asignacionService.generarRanking(donacion))
                    .thenReturn(matchmaking(List.of(), List.of(resultado(1.0)), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            InOrder orden = inOrder(candidatosRepositorio);
            orden.verify(candidatosRepositorio).limpiar();
            orden.verify(candidatosRepositorio).guardar(any());
        }

        @Test
        @DisplayName("Limpia la corrida anterior aunque esta vez no haya donaciones en depósito")
        void limpiaAunSinDonaciones() {
            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO")).thenReturn(List.of());

            servicio.ejecutarMatchmakingNocturno();

            verify(candidatosRepositorio).limpiar();
        }
    }

    @Nested
    @DisplayName("Tolerancia a errores: una donación mala no frena el batch")
    class ToleranciaAErrores {

        @Test
        @DisplayName("Si una donación falla, se saltea y el proceso sigue con las demás")
        void unaFallaNoDetieneElProceso() {
            // Es un proceso de madrugada sin nadie mirando: no puede caerse entero
            // porque una donación tenga datos incompletos.
            Donacion rota = donacionEnDeposito("arroz");
            Donacion sana = donacionEnDeposito("ropa");

            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO")).thenReturn(List.of(rota, sana));
            when(asignacionService.generarRanking(rota)).thenThrow(new RuntimeException("datos inconsistentes"));
            when(asignacionService.generarRanking(sana))
                    .thenReturn(matchmaking(List.of(), List.of(resultado(1.0)), List.of()));

            assertThatCode(() -> servicio.ejecutarMatchmakingNocturno()).doesNotThrowAnyException();

            // Solo se guarda la que se pudo calcular.
            verify(candidatosRepositorio, times(1)).guardar(rankingCaptor.capture());
            assertThat(rankingCaptor.getValue().getIdDonacion()).isEqualTo(sana.getId());
        }
    }

    @Nested
    @DisplayName("El proceso no asigna por su cuenta")
    class NoAsignaAutomaticamente {

        @Test
        @DisplayName("Aunque haya coincidencia entre ambos algoritmos, la donación sigue EN_DEPOSITO")
        void noCambiaElEstadoDeLaDonacion() {
            // El enunciado pide que sea "una persona administradora" quien confirme
            // el destino final. El batch solo deja la recomendación lista.
            Donacion donacion = donacionEnDeposito("arroz");
            when(donacionesRepositorio.obtenerPorEstado("EN_DEPOSITO")).thenReturn(List.of(donacion));
            when(asignacionService.generarRanking(donacion))
                    .thenReturn(matchmaking(List.of(resultado(3.0)), List.of(resultado(3.0)), List.of(resultado(1.0))));

            servicio.ejecutarMatchmakingNocturno();

            assertThat(donacion.estaEnEstado("EN_DEPOSITO")).isTrue();
            assertThat(donacion.getIdEntidadBeneficiaria()).isNull();
            assertThat(donacion.getHistorialEstados()).isEmpty();
        }
    }
}
