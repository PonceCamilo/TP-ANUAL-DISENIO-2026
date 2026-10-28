package ar.utn.donatrack.donaciones.config;

/**
 * Las propiedades del servicio con sus valores por defecto, para armar services
 * en tests unitarios sin levantar Spring. DonacionesPropertiesTest verifica que
 * coincidan con los @DefaultValue de DonacionesProperties.
 */
public final class PropiedadesDePrueba {

    private PropiedadesDePrueba() {
    }

    public static DonacionesProperties porDefecto() {
        return new DonacionesProperties(
                new DonacionesProperties.Inactividad(20, "0 0 0 * * *", 100),
                new DonacionesProperties.Procesos("0 0 3 * * *", "0 30 3 * * *", "0 0 4 * * *"),
                new DonacionesProperties.Importacion(500, 100),
                new DonacionesProperties.Notificaciones("admin@donatrack.org"),
                new DonacionesProperties.Matchmaking(10),
                new DonacionesProperties.Salida(true, 5000, 50, 10, 7, "0 30 4 * * *"));
    }
}
