package ar.utn.donatrack.incentivos.interfaces.services;
import java.util.UUID;

public interface MisionesQueryPort {
    int contarMisionesCompletadasEnPeriodo(UUID donanteId, int mes, int anio);
}
