package ar.utn.donatrack.incentivos.interfaces.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DonacionesQueryPort{
        List<DonacionResumen> obtenerDonacionesDe(UUID donanteId);
        List<UUID> obtenerTodosLosDonantesIds();

        record DonacionResumen(UUID entidadBeneficiariaId, LocalDate fecha) {} //ENTENDER QUE ES RECORD
}
