package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.interfaces.services.MisionesQueryPort;
import ar.utn.donatrack.incentivos.repositories.impl.IncentivosRepositorioEnMemoria;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Adapter (puerto de consulta) sobre las misiones completadas por un donante.
 *
 * Cada misión completada otorga una InsigniaObtenida con su fechaObtencion.
 * Por eso contar las misiones completadas en un período equivale a contar
 * las insignias obtenidas dentro de ese mes/año. Lo usa el RankingMensualJob.
 */
@Service
public class MisionesQueryPortImpl implements MisionesQueryPort {

    private final IncentivosRepositorioEnMemoria repositorio;

    public MisionesQueryPortImpl(IncentivosRepositorioEnMemoria repositorio) {
        this.repositorio = repositorio;
    }

    public int contarMisionesCompletadasEnPeriodo(UUID donanteId, int mes, int anio) {
        return repositorio.buscarPerfil(donanteId)
                .map(donante -> (int) donante.getInsigniasObtenidas().stream()
                        .filter(insignia -> insignia.getFechaObtencion() != null
                                && insignia.getFechaObtencion().getMonthValue() == mes
                                && insignia.getFechaObtencion().getYear() == anio)
                        .count())
                .orElse(0);
    }
}
