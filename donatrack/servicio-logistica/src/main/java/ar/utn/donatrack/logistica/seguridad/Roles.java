package ar.utn.donatrack.logistica.seguridad;

/** Valores del claim `rol` del JWT de Autenticación, más el de los servicios con clave compartida. */
public final class Roles {
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String CHOFER = "CHOFER";
    public static final String ENTIDAD_BENEFICIARIA = "ENTIDAD_BENEFICIARIA";
    public static final String DONANTE = "DONANTE";
    /** No viene de un token: lo recibe quien manda la clave entre servicios (el broker). */
    public static final String SERVICIO = "SERVICIO";

    private Roles() {}
}
