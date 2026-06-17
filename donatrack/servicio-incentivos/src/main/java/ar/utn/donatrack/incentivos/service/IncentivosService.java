package ar.utn.donatrack.incentivos.service;

import ar.utn.donatrack.incentivos.model.CategoriaDonante;
import ar.utn.donatrack.incentivos.model.Insignia;
import ar.utn.donatrack.incentivos.model.PerfilDonante;
import ar.utn.donatrack.incentivos.model.ProgresoMision;

import java.util.List;
import java.util.UUID;

public interface IncentivosService {

    /** Retorna métricas acumuladas y del período actual del donante. */
    PerfilDonante obtenerMetricas(UUID donanteId);

    /** Lista los progresos de misiones disponibles para el donante. */
    List<ProgresoMision> obtenerMisiones(UUID donanteId);

    /** Lista las insignias obtenidas por el donante. */
    List<Insignia> obtenerInsignias(UUID donanteId);

    /**
     * Llamado por servicio-donaciones cuando el donante registra una donación.
     * Actualiza métricas y progreso de misiones.
     *
     * @param donanteId        id del donante
     * @param destinatario     contacto del donante (para notificarlo)
     * @param medio            medio de contacto ("EMAIL", "SMS", "WHATSAPP")
     * @param cantidadBienes   cantidad de bienes donados
     * @param categoriasDonadas categorías de los bienes donados
     */
    void procesarDonacion(UUID donanteId, String destinatario, String medio,
                          int cantidadBienes, List<String> categoriasDonadas);

    /**
     * Llamado por servicio-donaciones cuando una donación es entregada exitosamente.
     */
    void procesarDonacionExitosa(UUID donanteId, String destinatario, String medio);
}
