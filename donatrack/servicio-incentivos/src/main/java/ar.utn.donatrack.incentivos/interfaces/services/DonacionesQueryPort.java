//TODO
package ar.utn.donatrack.incentivos.interfaces.services;

public class DonacionesQueryPort {
    List<DonacionResumen> obtenerDonacionesDe(UUID donanteId);
    List<UUID> obtenerTodosLosDonantesIds();

    record DonacionResumen(UUID entidadBeneficiariaId, LocalDate fecha) {} //ENTENDER QUE ES RECORD
}
