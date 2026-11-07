package ar.utn.donatrack.incentivos.jobs;

import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.interfaces.services.IncentivosServiceInterface;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IncentivosJob - tareas programadas")
class IncentivosJobTest {

    private final IncentivosServiceInterface incentivosService = mock(IncentivosServiceInterface.class);
    private final RankingMensualRepositoryInterface rankingRepository = mock(RankingMensualRepositoryInterface.class);
    private final IncentivosJob job = new IncentivosJob(incentivosService, rankingRepository);

    @Test
    @DisplayName("Revisar rachas delega en el servicio")
    void revisaRachas() {
        job.revisarRachasCadaTreintaDias();

        verify(incentivosService).revisarRachas();
    }

    @Test
    @DisplayName("Calcular ranking mensual guarda el ranking actual")
    void guardaRankingMensual() {
        RankingMensual ranking = RankingMensual.builder()
                .periodoInicio(LocalDateTime.of(2026, 6, 1, 0, 0))
                .fechaCalculo(LocalDateTime.of(2026, 6, 30, 23, 59))
                .posiciones(List.of())
                .build();
        when(incentivosService.obtenerRankingMensualActual()).thenReturn(ranking);

        job.calcularRankingMensual();

        verify(rankingRepository).guardar(ranking);
    }
}
