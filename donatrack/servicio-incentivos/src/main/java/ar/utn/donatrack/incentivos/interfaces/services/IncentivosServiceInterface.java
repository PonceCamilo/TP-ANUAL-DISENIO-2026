package ar.utn.donatrack.incentivos.interfaces.services;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.DonacionRegistrada;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Mision;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IncentivosServiceInterface {

    Donante obtenerPerfil(UUID donanteId);

    // Lista los progresos de misiones disponibles para el donante.
    List<Mision> obtenerMisiones(UUID donanteId);

    // Lista las insignias obtenidas por el donante.
    List<InsigniaObtenida> obtenerInsignias(UUID donanteId);

    RankingMensual obtenerRankingMensualActual();

    int obtenerPosicionRankingActual(UUID donanteId);

    void revisarRachas();

    void procesarDonacion(UUID donanteId, DonacionRegistrada donacion, String destinatario, String medio);

    // Llamado por servicio-donaciones cuando una donación es entregada exitosamente.
    void procesarDonacionExitosa(UUID donanteId, DonacionRegistrada donacion, String destinatario, String medio);

    // Entrega 6: el nombre con que se muestra a la persona en el podio y el ranking.
    // Lo manda Donaciones en cada aviso; vacío, no cambia el que ya tenía.
    void actualizarNombreVisible(UUID donanteId, String nombreVisible);

    Map<UUID, String> nombresVisibles(Collection<UUID> donanteIds);
}
