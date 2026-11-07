package ar.utn.donatrack.donaciones.salida;

/** A dónde va cada mensaje de la bandeja de salida: cada destino tiene su enviador. */
public final class Destinos {

    public static final String NOTIFICACION = "NOTIFICACION";
    public static final String INCENTIVOS_DONACION_REGISTRADA = "INCENTIVOS_DONACION_REGISTRADA";
    public static final String INCENTIVOS_DONACION_EXITOSA = "INCENTIVOS_DONACION_EXITOSA";

    private Destinos() {
    }
}
