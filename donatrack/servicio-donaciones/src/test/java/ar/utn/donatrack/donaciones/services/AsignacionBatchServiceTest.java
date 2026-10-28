package ar.utn.donatrack.donaciones.services;

import org.springframework.transaction.support.TransactionOperations;
import org.junit.jupiter.api.BeforeEach;
import ar.utn.donatrack.donaciones.config.PropiedadesDePrueba;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests del proceso nocturno de matchmaking.
 *
 * El enunciado (Entrega 2) pide ejecutar los algoritmos de asignación sobre
 * TODAS las donaciones que están EN_DEPOSITO, y hacerlo "en horarios de baja
 * carga" para no degradar el desempeño. Este servicio es esa ejecución.
 *
 * Decisión de diseño que estos tests fijan: el proceso NO asigna por su cuenta.
 * Calcula los rankings y reemplaza la bandeja; la asignación efectiva sigue
 * siendo una acción explícita de una persona administradora (DonacionComandosService.asignar).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AsignacionBatchService - matchmaking nocturno de las donaciones en depósito")
class AsignacionBatchServiceTest {

    @Mock
    private RepositorioDonaciones donacionesRepositorio;

    @Mock
    private AsignacionDonacionesService asignacionService;

    @Mock
    private RepositorioCandidatosAsignacion candidatosRepositorio;

    @Mock
    private RepositorioEntidadesBeneficiarias entidadesRepositorio;

    private AsignacionBatchService servicio;

    @Captor
    private ArgumentCaptor<Collection<RankingPrecalculado>> bandejaCaptor;

    @BeforeEach
    void crearServicio() {
        servicio = servicioConLotesDe(100);
    }

    /** El proceso real, sin transacciones (los repositorios son simulados) y con el tamaño de lote indicado. */
    private AsignacionBatchService servicioConLotesDe(int tamanio) {
        return new AsignacionBatchService(donacionesRepositorio, asignacionService, candidatosRepositorio,
                entidadesRepositorio, PropiedadesDePrueba.conLoteDeProcesos(tamanio),
                TransactionOperations.withoutTransaction());
    }

    /** Donación EN_DEPOSITO de la subcategoría indicada. */
    private Donacion donacionEnDeposito(String subcategoria) {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(UUID.randomUUID());
        donacion.setSubcategoria(new Subcategoria(subcategoria));
        return donacion;
    }

    /** Resultado de matchmaking con el ranking de cada uno de los dos algoritmos. */
    private ResultadoMatchmaking matchmaking(List<ResultadoAsignacion> semantico, List<ResultadoAsignacion> subAtendidos) {
        Map<String, List<ResultadoAsignacion>> rankings = new LinkedHashMap<>();
        rankings.put(AlgoritmoCompatibilidadSemantica.NOMBRE, semantico);
        rankings.put(AlgoritmoPrioridadSubAtendidos.NOMBRE, subAtendidos);
        return new ResultadoMatchmaking(rankings);
    }

    private ResultadoAsignacion resultado(double puntaje) {
        return new ResultadoAsignacion(UUID.randomUUID(), puntaje);
    }

    /** La bandeja que el proceso dejó guardada (una sola llamada a reemplazarTodos). */
    private List<RankingPrecalculado> bandejaGuardada() {
        verify(candidatosRepositorio).reemplazarTodos(bandejaCaptor.capture());
        return new ArrayList<>(bandejaCaptor.getValue());
    }

    @Nested
    @DisplayName("Alcance del proceso")
    class Alcance {

        @Test
        @DisplayName("Solo procesa las donaciones que están EN_DEPOSITO")
        void soloLasDeDeposito() {
            // Una donación ya asignada o entregada no vuelve al matchmaking:
            // se consulta al repositorio filtrando por estado, de a lotes.
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100)).thenReturn(List.of());

            servicio.ejecutarMatchmakingNocturno();

            verify(donacionesRepositorio).obtenerLoteEnEstado("EN_DEPOSITO", null, 100);
            verifyNoMoreInteractions(donacionesRepositorio);
        }

        @Test
        @DisplayName("Calcula un ranking por cada donación en depósito y los guarda todos juntos")
        void unRankingPorDonacion() {
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100))
                    .thenReturn(List.of(donacionEnDeposito("arroz"), donacionEnDeposito("ropa")));
            when(asignacionService.generarRanking(any(), any()))
                    .thenReturn(matchmaking(List.of(resultado(1.0)), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            verify(asignacionService, times(2)).generarRanking(any(), any());
            assertThat(bandejaGuardada()).hasSize(2);
        }

        @Test
        @DisplayName("Las donaciones con bienes vencidos quedan fuera de la bandeja")
        void dejaAfueraLasVencidas() {
            Donacion vencida = donacionEnDeposito("leche");
            vencida.agregarBien(BienPerecible.builder().subcategoria(new Subcategoria("leche"))
                    .descripcion("Leche").cantidad(1).unidad("litros")
                    .fechaVencimiento(java.time.LocalDate.of(2020, 1, 1)).build());
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100))
                    .thenReturn(List.of(vencida, donacionEnDeposito("arroz")));
            when(asignacionService.generarRanking(any(), any()))
                    .thenReturn(matchmaking(List.of(resultado(1.0)), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            verify(asignacionService, times(1)).generarRanking(any(), any());
            verify(asignacionService, never()).generarRanking(eq(vencida), any());
        }

        @Test
        @DisplayName("Carga las entidades una vez por lote, no una vez por donación")
        void cargaLasEntidadesUnaVezPorLote() {
            // Entrega 5: antes cada donación volvía a leer todas las entidades.
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100))
                    .thenReturn(List.of(donacionEnDeposito("arroz"), donacionEnDeposito("ropa")));
            when(asignacionService.generarRanking(any(), any()))
                    .thenReturn(matchmaking(List.of(), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            verify(entidadesRepositorio, times(1)).buscarTodas();
        }

        @Test
        @DisplayName("Recorre las donaciones de a lotes: cada lote sigue desde la última del anterior")
        void recorreDeALotes() {
            servicio = servicioConLotesDe(2);
            Donacion primera = donacionEnDeposito("arroz");
            Donacion segunda = donacionEnDeposito("ropa");
            Donacion tercera = donacionEnDeposito("leche");
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 2)).thenReturn(List.of(primera, segunda));
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", segunda.getId(), 2)).thenReturn(List.of(tercera));
            when(asignacionService.generarRanking(any(), any())).thenReturn(matchmaking(List.of(), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            // Un lote incompleto es el último: no se pide otro. La bandeja se guarda una vez, con todo.
            assertThat(bandejaGuardada()).extracting(RankingPrecalculado::getIdDonacion)
                    .containsExactly(primera.getId(), segunda.getId(), tercera.getId());
            verify(entidadesRepositorio, times(2)).buscarTodas();
        }

        @Test
        @DisplayName("Sin donaciones en depósito deja la bandeja vacía y no falla")
        void sinDonaciones() {
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100)).thenReturn(List.of());

            assertThatCode(() -> servicio.ejecutarMatchmakingNocturno()).doesNotThrowAnyException();

            assertThat(bandejaGuardada()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Contenido del ranking guardado")
    class ContenidoDelRanking {

        @Test
        @DisplayName("Guarda el ranking de cada algoritmo junto al id de la donación y la fecha")
        void guardaLosRankings() {
            Donacion donacion = donacionEnDeposito("arroz");
            ResultadoAsignacion ambos = resultado(3.0);

            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100)).thenReturn(List.of(donacion));
            when(asignacionService.generarRanking(eq(donacion), any()))
                    .thenReturn(matchmaking(List.of(ambos, resultado(1.0)), List.of(ambos)));

            servicio.ejecutarMatchmakingNocturno();

            RankingPrecalculado guardado = bandejaGuardada().getFirst();
            assertThat(guardado.getIdDonacion()).isEqualTo(donacion.getId());
            assertThat(guardado.resultado().ranking(AlgoritmoCompatibilidadSemantica.NOMBRE)).hasSize(2);
            assertThat(guardado.resultado().ranking(AlgoritmoPrioridadSubAtendidos.NOMBRE)).hasSize(1);
            assertThat(guardado.resultado().coincidencias()).extracting(ResultadoAsignacion::getIdEntidad)
                    .containsExactly(ambos.getIdEntidad());
            assertThat(guardado.getFechaCalculo()).isNotNull();
        }

        @Test
        @DisplayName("huboCoincidencias() es true cuando todos los algoritmos recomiendan la misma entidad")
        void detectaCoincidencias() {
            ResultadoAsignacion ambos = resultado(3.0);
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100))
                    .thenReturn(List.of(donacionEnDeposito("arroz")));
            when(asignacionService.generarRanking(any(), any()))
                    .thenReturn(matchmaking(List.of(ambos), List.of(resultado(1.0), ambos)));

            servicio.ejecutarMatchmakingNocturno();

            assertThat(bandejaGuardada().getFirst().huboCoincidencias()).isTrue();
        }

        @Test
        @DisplayName("sinCandidatas() es true cuando ningún algoritmo devolvió entidades")
        void detectaDonacionSinCandidatas() {
            // Pasa cuando ninguna entidad declaró una necesidad compatible: la
            // donación se queda en depósito y el administrador lo ve en la bandeja.
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100))
                    .thenReturn(List.of(donacionEnDeposito("bicicletas")));
            when(asignacionService.generarRanking(any(), any()))
                    .thenReturn(matchmaking(List.of(), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            assertThat(bandejaGuardada().getFirst().sinCandidatas()).isTrue();
        }
    }

    @Nested
    @DisplayName("Vigencia de los resultados")
    class Vigencia {

        @Test
        @DisplayName("Reemplaza la bandeja entera una sola vez, al final de la corrida")
        void reemplazaUnaSolaVez() {
            // Los rankings de la corrida anterior se descartan, pero de una vez y al
            // final: si el proceso se cortara a mitad, la bandeja de ayer sigue entera.
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100))
                    .thenReturn(List.of(donacionEnDeposito("arroz"), donacionEnDeposito("ropa")));
            when(asignacionService.generarRanking(any(), any()))
                    .thenReturn(matchmaking(List.of(resultado(1.0)), List.of()));

            servicio.ejecutarMatchmakingNocturno();

            verify(candidatosRepositorio, times(1)).reemplazarTodos(any());
            verifyNoMoreInteractions(candidatosRepositorio);
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

            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100)).thenReturn(List.of(rota, sana));
            when(asignacionService.generarRanking(eq(rota), any())).thenThrow(new RuntimeException("datos inconsistentes"));
            when(asignacionService.generarRanking(eq(sana), any()))
                    .thenReturn(matchmaking(List.of(resultado(1.0)), List.of()));

            assertThatCode(() -> servicio.ejecutarMatchmakingNocturno()).doesNotThrowAnyException();

            // Solo queda en la bandeja la que se pudo calcular.
            assertThat(bandejaGuardada()).extracting(RankingPrecalculado::getIdDonacion).containsExactly(sana.getId());
        }
    }

    @Nested
    @DisplayName("El proceso no asigna por su cuenta")
    class NoAsignaAutomaticamente {

        @Test
        @DisplayName("Aunque haya coincidencia entre los algoritmos, la donación sigue EN_DEPOSITO")
        void noCambiaElEstadoDeLaDonacion() {
            // El enunciado pide que sea "una persona administradora" quien confirme
            // el destino final. El batch solo deja la recomendación lista.
            Donacion donacion = donacionEnDeposito("arroz");
            ResultadoAsignacion ambos = resultado(3.0);
            when(donacionesRepositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 100)).thenReturn(List.of(donacion));
            when(asignacionService.generarRanking(eq(donacion), any()))
                    .thenReturn(matchmaking(List.of(ambos), List.of(ambos)));

            servicio.ejecutarMatchmakingNocturno();

            assertThat(donacion.estaEnEstado("EN_DEPOSITO")).isTrue();
            assertThat(donacion.getIdEntidadBeneficiaria()).isNull();
            assertThat(donacion.getHistorialEstados()).isEmpty();
        }
    }
}
