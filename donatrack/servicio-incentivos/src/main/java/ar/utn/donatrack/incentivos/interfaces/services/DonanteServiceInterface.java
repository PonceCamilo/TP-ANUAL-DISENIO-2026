package ar.utn.donatrack.incentivos.interfaces.services;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import java.util.UUID;

public interface DonanteServiceInterface {
    void procesarAvance(UUID idDonante);
    void completarMision(Donante donante, Mision mision);
    void subirDeCategoria(Donante donante);
    void cambiarVisibilidadInsignia(UUID idDonante, UUID idInsigniaObtenida, boolean visible);
    int obtenerProgresoMisionActual(UUID idDonante);
    int obtenerDistanciaRestanteMisionActual(UUID idDonante);
}