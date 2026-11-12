package ar.utn.donatrack.donaciones.config;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Completa el esquema de una base creada antes de las migraciones con una
 * versión de las entidades anterior a la de la Entrega 5 (Entrega 6).
 *
 * El problema: una base que ya tenía tablas al introducir Flyway se marca en la
 * versión 1 sin ejecutar V1 (spring.flyway.baseline-on-migrate), dando por hecho
 * que ya tiene ese esquema. Eso vale para una base de la Entrega 5, pero no para
 * una de la Entrega 4: le faltan, por ejemplo, ranking_precalculado y
 * entidad_recepcion_diaria, y V2 falla al referenciarlas. El servicio no
 * arrancaba y la única salida era borrar la base.
 *
 * V1 y V2 no se pueden corregir: ya están aplicadas en otras bases y Flyway
 * rechaza el arranque si cambia el checksum de una migración aplicada. Por eso
 * se resuelve con este callback, que ejecuta db/reparacion/completar-esquema-v1.sql
 * (todo con IF NOT EXISTS) solo cuando la base está marcada en la versión 1 por
 * baseline y todavía no tiene aplicada la 2:
 *
 *   - AFTER_BASELINE: la base vieja se acaba de marcar en la versión 1.
 *   - BEFORE_MIGRATE: la base ya se había marcado en un arranque anterior que
 *     falló en V2 (PostgreSQL deshace V2, pero la marca de baseline queda).
 *
 * En una base nueva no corre: V1 se ejecuta entera y no hay baseline. En una que
 * ya tiene la versión 2 tampoco: V2 borró tablas de V1 y no hay que recrearlas.
 */
@Component
public class CompletarEsquemaDeBaseVieja implements Callback {

    private static final Logger log = LoggerFactory.getLogger(CompletarEsquemaDeBaseVieja.class);

    static final String SCRIPT = "db/reparacion/completar-esquema-v1.sql";

    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_BASELINE || event == Event.BEFORE_MIGRATE;
    }

    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    public void handle(Event event, Context context) {
        try {
            Connection conexion = context.getConnection();
            String historial = context.getConfiguration().getTable();
            if (!marcadaEnV1SinV2(conexion, historial)) {
                return;
            }
            List<String> sentencias = sentenciasDelScript();
            try (Statement sentencia = conexion.createStatement()) {
                for (String sql : sentencias) {
                    sentencia.execute(sql);
                }
            }
            log.warn("Base creada antes de las migraciones con un esquema anterior a la Entrega 5: "
                    + "se completó el esquema de la versión 1 ({} sentencias idempotentes)", sentencias.size());
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo completar el esquema de la versión 1", e);
        }
    }

    public String getCallbackName() {
        return "completar-esquema-de-base-vieja";
    }

    /** La base tiene la marca de baseline en la versión 1 y no tiene aplicada la versión 2. */
    private boolean marcadaEnV1SinV2(Connection conexion, String historial) throws SQLException {
        if (!existeTabla(conexion, historial)) {
            return false;
        }
        // Flyway crea el historial con nombres entre comillas y en minúsculas, así
        // que hay que citarlos igual para que la consulta funcione también en H2.
        String sql = "select "
                + "sum(case when \"type\" = 'BASELINE' and \"version\" = '1' then 1 else 0 end), "
                + "sum(case when \"version\" = '2' and \"success\" = true then 1 else 0 end) "
                + "from \"" + historial + "\"";
        try (PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultado = consulta.executeQuery()) {
            return resultado.next() && resultado.getLong(1) > 0 && resultado.getLong(2) == 0;
        }
    }

    private boolean existeTabla(Connection conexion, String tabla) throws SQLException {
        DatabaseMetaData metadatos = conexion.getMetaData();
        for (String nombre : List.of(tabla, tabla.toUpperCase())) {
            try (ResultSet tablas = metadatos.getTables(conexion.getCatalog(), conexion.getSchema(), nombre, null)) {
                if (tablas.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<String> sentenciasDelScript() {
        try (InputStream entrada = CompletarEsquemaDeBaseVieja.class.getClassLoader().getResourceAsStream(SCRIPT)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta el recurso " + SCRIPT);
            }
            String sinComentarios = new String(entrada.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(linea -> !linea.strip().startsWith("--"))
                    .collect(Collectors.joining("\n"));
            return Arrays.stream(sinComentarios.split(";"))
                    .map(String::strip)
                    .filter(sentencia -> !sentencia.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
