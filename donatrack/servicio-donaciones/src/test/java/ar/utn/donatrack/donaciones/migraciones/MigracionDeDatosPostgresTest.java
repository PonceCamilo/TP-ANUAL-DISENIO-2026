package ar.utn.donatrack.donaciones.migraciones;

import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Los mismos casos de MigracionDeDatosTest contra PostgreSQL 16, la base que usa
 * el servicio: H2 en modo PostgreSQL no tiene exactamente el mismo dialecto.
 * Cada test migra en un esquema propio. Sin Docker disponible, se saltea.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Migraciones en PostgreSQL 16: los datos de la Entrega 5 pasan a la forma nueva")
class MigracionDeDatosPostgresTest extends MigracionDeDatosTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Override
    protected String baseVacia() throws SQLException {
        String esquema = "migracion_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection conexion = DriverManager.getConnection(POSTGRES.getJdbcUrl(), usuario(), clave());
             Statement sentencia = conexion.createStatement()) {
            sentencia.execute("create schema " + esquema);
        }
        String url = POSTGRES.getJdbcUrl();
        return url + (url.contains("?") ? "&" : "?") + "currentSchema=" + esquema;
    }

    @Override
    protected String usuario() {
        return POSTGRES.getUsername();
    }

    @Override
    protected String clave() {
        return POSTGRES.getPassword();
    }
}
