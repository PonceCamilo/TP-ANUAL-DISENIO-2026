package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.interfaces.services.DonacionesQueryPort;
import ar.utn.donatrack.incentivos.repositories.impl.IncentivosRepositorioEnMemoria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Adapter (puerto de consulta) sobre los datos de donaciones que conoce incentivos.
 *
 * Incentivos no persiste cada donación individual: las métricas agregadas viven
 * en el propio Donante (totalDonacionesHistoricas, organizacionesAyudadas, etc.),
 * que se actualizan vía los endpoints de integración con servicio-donaciones.
 * Por eso obtenerDonacionesDe() devuelve una lista vacía: el detalle por donación
 * no se almacena de este lado. obtenerTodosLosDonantesIds() sí devuelve los
 * perfiles conocidos, que es lo que necesita el RankingMensualJob.
 */
@Service
public class DonacionesQueryPortImpl implements DonacionesQueryPort {

    private final IncentivosRepositorioEnMemoria repositorio;

    public DonacionesQueryPortImpl(IncentivosRepositorioEnMemoria repositorio) {
        this.repositorio = repositorio;
    }

    public List<DonacionResumen> obtenerDonacionesDe(UUID donanteId) {
        return List.of();
    }

    public List<UUID> obtenerTodosLosDonantesIds() {
        return repositorio.listarTodosLosDonanteIds();
    }
}
