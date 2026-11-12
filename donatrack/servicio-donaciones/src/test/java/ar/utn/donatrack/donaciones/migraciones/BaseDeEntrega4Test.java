package ar.utn.donatrack.donaciones.migraciones;

import ar.utn.donatrack.donaciones.config.CompletarEsquemaDeBaseVieja;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Una base creada por Hibernate en la Entrega 4, antes de que existieran las
 * migraciones, tiene que poder migrarse sin borrarla.
 *
 * Flyway la marca en la versión 1 sin ejecutar V1 (baseline), pero le faltan
 * tablas de la Entrega 5 que V2 necesita. CompletarEsquemaDeBaseVieja las crea
 * antes de V2. Flyway se configura igual que en application.properties.
 */
@DisplayName("Migraciones: una base de la Entrega 4 se completa y migra")
class BaseDeEntrega4Test {

    private String url;

    private final UUID persona = UUID.randomUUID();
    private final UUID entidad = UUID.randomUUID();
    private final UUID campania = UUID.randomUUID();

    @BeforeEach
    void baseNueva() {
        url = "jdbc:h2:mem:entrega4_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
    }

    private Flyway comoLaApp() {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .callbacks(new CompletarEsquemaDeBaseVieja())
                .load();
    }

    private Flyway sinReparacion() {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }

    private Connection conexion() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    /** Crea el esquema de la Entrega 4 y le carga un donante y una entidad con una campaña. */
    private void baseDeLaEntrega4() throws Exception {
        String script;
        try (InputStream entrada = getClass().getClassLoader().getResourceAsStream("migraciones/esquema-entrega-4.sql")) {
            script = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection c = conexion(); Statement s = c.createStatement()) {
            for (String sql : script.lines().filter(l -> !l.startsWith("--")).reduce("", (a, b) -> a + "\n" + b).split(";")) {
                if (!sql.isBlank()) {
                    s.execute(sql);
                }
            }
            ejecutar(c, "insert into persona_donante (id, email, estado) values (?, 'juan@example.com', 'ACTIVO')", persona);
            ejecutar(c, "insert into persona_humana (id, nombre, apellido) values (?, 'Juan', 'Pérez')", persona);
            ejecutar(c, "insert into entidad_beneficiaria (id, razon_social) values (?, 'Comedor Los Pibes')", entidad);
            ejecutar(c, "insert into campania (id_campania, entidad_id, id_entidad, descripcion_general) "
                    + "values (?, ?, ?, 'Invierno')", campania, entidad, entidad);
        }
    }

    private void ejecutar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            sentencia.executeUpdate();
        }
    }

    private long contar(String sql, Object... parametros) throws SQLException {
        try (Connection c = conexion(); PreparedStatement sentencia = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            try (ResultSet r = sentencia.executeQuery()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    private boolean existeTabla(String tabla) throws SQLException {
        return contar("select count(*) from information_schema.tables where lower(table_name) = ?", tabla) > 0;
    }

    /** "tabla.columna" de todas las tablas de la aplicación (sin el historial de Flyway). */
    private static Set<String> columnas(String url) throws SQLException {
        Set<String> columnas = new HashSet<>();
        try (Connection c = DriverManager.getConnection(url, "sa", "");
             ResultSet r = c.createStatement().executeQuery(
                     "select lower(table_name), lower(column_name) from information_schema.columns "
                             + "where table_schema = 'PUBLIC' and lower(table_name) <> 'flyway_schema_history'")) {
            while (r.next()) {
                columnas.add(r.getString(1) + "." + r.getString(2));
            }
        }
        return columnas;
    }

    private String ultimaVersion(Flyway flyway) {
        MigrationInfo actual = flyway.info().current();
        return actual.getVersion().getVersion();
    }

    @Test
    @DisplayName("Migra hasta la última versión conservando los datos")
    void migraConservandoLosDatos() throws Exception {
        baseDeLaEntrega4();

        Flyway flyway = comoLaApp();
        flyway.migrate();

        assertThat(ultimaVersion(flyway)).isEqualTo("5");
        assertThat(contar("select count(*) from persona_donante where id = ?", persona)).isOne();
        assertThat(contar("select count(*) from persona_humana where id = ? and nombre = 'Juan'", persona)).isOne();
        assertThat(contar("select count(*) from campania where id_campania = ? and entidad_id = ?", campania, entidad))
                .isOne();
    }

    @Test
    @DisplayName("Queda con todas las columnas que tiene una base creada desde cero")
    void quedaConElEsquemaCompleto() throws Exception {
        baseDeLaEntrega4();
        comoLaApp().migrate();

        String urlNueva = "jdbc:h2:mem:desde_cero_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        Flyway.configure().dataSource(urlNueva, "sa", "").load().migrate();

        assertThat(columnas(url)).containsAll(columnas(urlNueva));
        assertThat(existeTabla("ranking_puesto")).isTrue();
        assertThat(existeTabla("entidad_recepcion_diaria")).isTrue();
    }

    @Test
    @DisplayName("Una columna que agregó la Entrega 5 a una tabla vieja también se completa")
    void completaColumnasFaltantes() throws Exception {
        baseDeLaEntrega4();
        try (Connection c = conexion(); Statement s = c.createStatement()) {
            s.execute("alter table persona_donante drop column ultima_interaccion");
        }

        comoLaApp().migrate();

        assertThat(contar("select count(*) from information_schema.columns "
                + "where lower(table_name) = 'persona_donante' and lower(column_name) = 'ultima_interaccion'")).isOne();
    }

    @Test
    @DisplayName("Sin la reparación, V2 falla en una base de la Entrega 4 (el problema que se corrige)")
    void sinReparacionFalla() throws Exception {
        baseDeLaEntrega4();

        Flyway flyway = sinReparacion();

        assertThatThrownBy(flyway::migrate)
                .hasStackTraceContaining("RANKING_PRECALCULADO");
    }

    @Test
    @DisplayName("Una base que quedó marcada en la versión 1 por un arranque fallido se recupera")
    void recuperaUnaBaseTrabada() throws Exception {
        baseDeLaEntrega4();
        // Lo que deja un arranque anterior que falló en V2 en PostgreSQL: la marca
        // de baseline queda y V2 se deshace.
        sinReparacion().baseline();

        Flyway flyway = comoLaApp();
        flyway.migrate();

        assertThat(ultimaVersion(flyway)).isEqualTo("5");
        assertThat(contar("select count(*) from persona_donante where id = ?", persona)).isOne();
    }

    @Test
    @DisplayName("En una base nueva no interviene: V1 se ejecuta entera y V2 borra las tablas reemplazadas")
    void baseNuevaNoUsaLaReparacion() throws Exception {
        Flyway flyway = comoLaApp();
        flyway.migrate();

        assertThat(contar("select count(*) from \"flyway_schema_history\" where \"type\" = 'BASELINE'")).isZero();
        assertThat(existeTabla("ranking_semantico")).isFalse();
    }

    @Test
    @DisplayName("En una base ya migrada no recrea las tablas que borró V2")
    void baseYaMigradaNoCambia() throws Exception {
        baseDeLaEntrega4();
        comoLaApp().migrate();

        comoLaApp().migrate();

        assertThat(existeTabla("ranking_semantico")).isFalse();
        assertThat(existeTabla("ranking_subatendidos")).isFalse();
    }

    @Test
    @DisplayName("Todas las sentencias del script de reparación son idempotentes")
    void scriptIdempotente() {
        assertThat(CompletarEsquemaDeBaseVieja.sentenciasDelScript())
                .isNotEmpty()
                .doesNotHaveDuplicates()
                .allSatisfy(sql -> assertThat(sql).containsAnyOf(
                        "create table if not exists", "add column if not exists", "create index if not exists"));
    }
}
