package ar.utn.donatrack.incentivos.interfaces.services;
import java.util.UUID;

public class MisionesQueryPort {
    int contarMisionesCompletadasEnPeriodo(UUID donanteId, int mes, int anio);
}
