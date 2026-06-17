package ar.utn.donatrack.incentivos.jobs;

import ar.utn.donatrack.incentivos.interfaces.ports.DonacionesQueryPort;
import ar.utn.donatrack.incentivos.interfaces.ports.MisionesQueryPort;
import ar.utn.donatrack.incentivos.interfaces.repositories.RankingMensualRepositoryInterface;
import ar.utn.donatrack.incentivos.models.PosicionRanking;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingMensualJob {

    private final DonacionesQueryPort donacionesQueryPort; //QueryPort es una arquitectura, es una interfaz (puente) que se usa exclusivamente para consultar datos, sin modificar nada.
    private final MisionesQueryPort misionesQueryPort;
    private final RankingMensualRepositoryInterface rankingRepository;

    // Corre a las 23:59 del último día de cada mes
    @Scheduled(cron = "0 59 23 L * ?")  // cron = "seg min hora L((last)=Último día del mes, ya sea 28, 30 o 31) mes(todos) ?(no importa que dia de la semana sea)"
    public void ejecutarRankingMensual() {
        LocalDate hoy = LocalDate.now();
        int mes = hoy.getMonthValue();
        int anio = hoy.getYear();

        List<UUID> donantesIds = donacionesQueryPort.obtenerTodosLosDonantesIds(); //esta funcion no esta implementada

        List<PosicionRanking> posiciones = donantesIds.stream()
                .map(donanteId -> {
                    int misiones = misionesQueryPort.contarMisionesCompletadasEnPeriodo(donanteId, mes, anio);
                    return PosicionRanking.builder()
                            .donanteId(donanteId)
                            .misionesCompletadas(misiones)
                            .build();
                })
                .sorted(Comparator.comparingInt(PosicionRanking::getMisionesCompletadas).reversed())
                .toList();

        // Asignamos el puesto después de ordenar
        for (int i = 0; i < posiciones.size(); i++) {
            posiciones.get(i).setPuesto(i + 1);
        }

        RankingMensual ranking = RankingMensual.builder()
                .mes(mes)
                .anio(anio)
                .fechaCalculo(hoy)
                .posiciones(posiciones)
                .build();

        rankingRepository.guardar(ranking);
    }
}