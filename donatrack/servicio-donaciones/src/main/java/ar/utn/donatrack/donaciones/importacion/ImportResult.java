package com.donatrack.donaciones.importacion;

/**
 * Resultado del procesamiento de una sola fila del CSV.
 * Se acumula en ImportReport al finalizar la importación.
 */
public class ImportResult {

    public enum Estado { CREADO, ACTUALIZADO, ERROR }

    private final int numeroLinea;
    private final Estado estado;
    private final String email;
    private final String detalle; // null si no hay error

    private ImportResult(int numeroLinea, Estado estado, String email, String detalle) {
        this.numeroLinea = numeroLinea;
        this.estado      = estado;
        this.email       = email;
        this.detalle     = detalle;
    }

    // ─── Métodos de fábrica ───────────────────────────────────────────────────

    public static ImportResult creado(int linea, String email) {
        return new ImportResult(linea, Estado.CREADO, email, null);
    }

    public static ImportResult actualizado(int linea, String email) {
        return new ImportResult(linea, Estado.ACTUALIZADO, email, null);
    }

    public static ImportResult error(int linea, String email, String motivo) {
        return new ImportResult(linea, Estado.ERROR, email, motivo);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public boolean esError()       { return estado == Estado.ERROR; }
    public int getNumeroLinea()    { return numeroLinea; }
    public Estado getEstado()      { return estado; }
    public String getEmail()       { return email; }
    public String getDetalle()     { return detalle; }

    @Override
    public String toString() {
        return "[%s] línea %d - %s%s".formatted(
                estado, numeroLinea, email,
                detalle != null ? ": " + detalle : "");
    }
}
