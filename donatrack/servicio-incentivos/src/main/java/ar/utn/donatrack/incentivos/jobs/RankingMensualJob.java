package ar.utn.donatrack.incentivos.jobs;

import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.interfaces.services.IncentivosServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingMensualJob {

    private final IncentivosServiceInterface incentivosService;
    private final RankingMensualRepositoryInterface rankingRepository;

    @Scheduled(cron = "0 59 23 L * ?")
    public void ejecutarRankingMensual() {
        rankingRepository.guardar(incentivosService.obtenerRankingMensualActual());
    }
}
