package ar.utn.donatrack.incentivos.model;

import java.util.UUID;

/**
 * Perfil de incentivos de un donante.
 * Almacena su categoría actual y sus métricas acumuladas.
 */
public class PerfilDonante {

    private final UUID donanteId;
    private CategoriaDonante categoria;
    private int totalDonacionesHistoricas;
    private int donacionesMesActual;
    private int organizacionesAyudadas;
    private int posicionRanking;

    public PerfilDonante(UUID donanteId) {
        this.donanteId               = donanteId;
        this.categoria               = CategoriaDonante.COLABORADOR; // inicia en la más baja
        this.totalDonacionesHistoricas = 0;
        this.donacionesMesActual     = 0;
        this.organizacionesAyudadas  = 0;
        this.posicionRanking         = 0;
    }

    public void registrarDonacion() {
        totalDonacionesHistoricas++;
        donacionesMesActual++;
    }

    public void registrarOrganizacionAyudada() {
        organizacionesAyudadas++;
    }

    /**
     * Sube de categoría si no está en el máximo.
     * @return true si efectivamente subió de categoría
     */
    public boolean subirCategoria() {
        return switch (categoria) {
            case COLABORADOR -> { categoria = CategoriaDonante.SOSTENEDOR; yield true; }
            case SOSTENEDOR  -> { categoria = CategoriaDonante.TRANSFORMADOR; yield true; }
            case TRANSFORMADOR -> false; // ya está en el máximo
        };
    }

    public void resetearDonacionesMes() { donacionesMesActual = 0; }
    public void setPosicionRanking(int pos) { posicionRanking = pos; }

    public UUID getDonanteId()                  { return donanteId; }
    public CategoriaDonante getCategoria()       { return categoria; }
    public int getTotalDonacionesHistoricas()    { return totalDonacionesHistoricas; }
    public int getDonacionesMesActual()          { return donacionesMesActual; }
    public int getOrganizacionesAyudadas()       { return organizacionesAyudadas; }
    public int getPosicionRanking()              { return posicionRanking; }
}
