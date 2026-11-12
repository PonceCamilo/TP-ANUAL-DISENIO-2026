package ar.utn.donatrack.donaciones.seguridad;

/**
 * Los roles que el Servicio de Autenticación escribe en el claim `rol` del token
 * (Entrega 6). Son los mismos nombres de su enum Rol: si cambian allá, cambian acá.
 *
 * El chofer existe en Autenticación pero no opera sobre Donaciones: sus pantallas
 * hablan con Logística, así que acá no tiene ningún permiso.
 */
public final class Roles {

    public static final String DONANTE = "DONANTE";
    public static final String ENTIDAD_BENEFICIARIA = "ENTIDAD_BENEFICIARIA";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String CHOFER = "CHOFER";

    private Roles() {
    }
}
