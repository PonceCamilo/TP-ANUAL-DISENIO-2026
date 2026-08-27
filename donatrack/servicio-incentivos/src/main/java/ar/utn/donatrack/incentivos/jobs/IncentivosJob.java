package ar.utn.donatrack.incentivos.jobs;

import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.interfaces.services.IncentivosServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncentivosJob {

    private final IncentivosServiceInterface incentivosService;
    private final RankingMensualRepositoryInterface rankingRepository;

    // Ejecutar cada 30 días a las 00:00:00
    @Scheduled(cron = "0 0 0 */30 * ?")
    public void revisarRachasCadaTreintaDias() {
        incentivosService.revisarRachas();
    }

    // 0 segundos, 59 minutos, 23 horas, último día del mes (L), todos los meses (*), cualquier día de la semana (?)
    @Scheduled(cron = "0 59 23 L * ?")
    public void calcularRankingMensual() {
        rankingRepository.guardar(incentivosService.obtenerRankingMensualActual());
    }
}
