package ar.utn.donatrack.donaciones.migraciones;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Una base con datos de la Entrega 5 (versión 1) migrada a la última versión:
 * los datos se conservan y quedan en la forma nueva. Es el caso de la base del
 * docker-compose, que ya tenía datos antes de las migraciones.
 *
 * Usa Flyway y JDBC directamente, sin Spring: primero migra hasta la versión 1,
 * inserta filas con el esquema viejo y después migra el resto.
 */
@DisplayName("Migraciones: los datos de la Entrega 5 pasan a la forma nueva")
class MigracionDeDatosTest {

    private final String url = "jdbc:h2:mem:migracion_datos_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

    private final UUID persona = UUID.randomUUID();
    private final UUID emailPredeterminado = UUID.randomUUID();
    private final UUID telefono = UUID.randomUUID();
    private final UUID donacion = UUID.randomUUID();
    private final UUID bien = UUID.randomUUID();
    private final UUID comedor = UUID.randomUUID();
    private final UUID escuela = UUID.randomUUID();

    private Flyway hasta(String version) {
        return Flyway.configure().dataSource(url, "sa", "").target(version).load();
    }

    private Connection conexion() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    private void ejecutar(Connection c, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            sentencia.executeUpdate();
        }
    }

    private List<List<Object>> consultar(String sql, Object... parametros) throws SQLException {
        try (Connection c = conexion(); PreparedStatement sentencia = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                sentencia.setObject(i + 1, parametros[i]);
            }
            List<List<Object>> filas = new ArrayList<>();
            try (ResultSet resultado = sentencia.executeQuery()) {
                int columnas = resultado.getMetaData().getColumnCount();
                while (resultado.next()) {
                    List<Object> fila = new ArrayList<>();
                    for (int i = 1; i <= columnas; i++) {
                        fila.add(resultado.getObject(i));
                    }
                    filas.add(fila);
                }
            }
            return filas;
        }
    }

    @BeforeEach
    void baseDeLaEntrega5ConDatos() throws SQLException {
        hasta("1").migrate();
        try (Connection c = conexion()) {
            // Persona con su medio predeterminado como referencia aparte (fuera de la lista).
            ejecutar(c, "insert into medio_de_contacto (id, tipo_medio, valor) values (?, 'EMAIL', 'ana@mail.com')",
                    emailPredeterminado);
            ejecutar(c, "insert into persona_donante (id, estado, email, tipo_documento, numero_documento, "
                    + "medio_predeterminado_id) values (?, 'ACTIVO', 'ana@mail.com', ' dni', '28.456.905', ?)",
                    persona, emailPredeterminado);
            ejecutar(c, "insert into persona_humana (id, nombre) values (?, 'Ana')", persona);
            ejecutar(c, "insert into medio_de_contacto (id, tipo_medio, valor, persona_donante_id) "
                    + "values (?, 'TELEFONO', '1155667788', ?)", telefono, persona);

            // Donación con la normalización vieja (solo minúsculas) y un proveedor mal escrito.
            ejecutar(c, "insert into donacion (id, estado, id_donante, subcategoria_tipo, subcategoria_tipo_normalizado, "
                    + "proveedor_logistica) values (?, 'EN_DEPOSITO', ?, 'Azúcar  Común', 'azúcar  común', 'donatrack ')",
                    donacion, persona);
            ejecutar(c, "insert into bien (id, tipo_bien, donacion_id, cantidad, subcategoria_tipo, "
                    + "subcategoria_tipo_normalizado) values (?, 'GENERICO', ?, 2, 'Azúcar  Común', 'azúcar  común')",
                    bien, donacion);

            // Bandeja de candidatos con las tres tablas de rankings.
            ejecutar(c, "insert into ranking_precalculado (id_donacion) values (?)", donacion);
            ejecutar(c, "insert into ranking_semantico (id_donacion, posicion, id_entidad, puntaje) values (?, 0, ?, 3.0)",
                    donacion, comedor);
            ejecutar(c, "insert into ranking_subatendidos (id_donacion, posicion, id_entidad, puntaje) values (?, 0, ?, 1.0)",
                    donacion, escuela);
            ejecutar(c, "insert into ranking_coincidencia (id_donacion, posicion, id_entidad, puntaje) values (?, 0, ?, 3.0)",
                    donacion, comedor);
        }
        hasta("latest").migrate();
    }

    @Test
    @DisplayName("El medio predeterminado pasa a ser un contacto marcado de la persona y la referencia vieja se va")
    void contactoPredeterminado() throws SQLException {
        assertThat(consultar("select id, predeterminado, persona_donante_id from medio_de_contacto "
                + "order by predeterminado desc"))
                .containsExactly(
                        List.of(emailPredeterminado, true, persona),
                        List.of(telefono, false, persona));
        assertThatThrownBy(() -> consultar("select medio_predeterminado_id from persona_donante"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    @DisplayName("La bandeja se conserva: cada ranking viejo pasa a puestos de su algoritmo y las coincidencias se van")
    void rankings() throws SQLException {
        assertThat(consultar("select algoritmo, posicion, id_entidad, puntaje from ranking_puesto where id_donacion = ? "
                + "order by algoritmo", donacion))
                .containsExactly(
                        List.of("COMPATIBILIDAD_SEMANTICA", 0, comedor, 3.0),
                        List.of("PRIORIDAD_SUBATENDIDOS", 0, escuela, 1.0));
        for (String vieja : List.of("ranking_semantico", "ranking_subatendidos", "ranking_coincidencia")) {
            assertThatThrownBy(() -> consultar("select count(*) from " + vieja)).isInstanceOf(SQLException.class);
        }
    }

    @Test
    @DisplayName("Las subcategorías se renormalizan sin acentos ni espacios repetidos, en donaciones y bienes")
    void subcategorias() throws SQLException {
        assertThat(consultar("select subcategoria_tipo, subcategoria_tipo_normalizado from donacion"))
                .containsExactly(List.of("Azúcar  Común", "azucar comun"));
        assertThat(consultar("select subcategoria_tipo_normalizado from bien")).containsExactly(List.of("azucar comun"));
    }

    @Test
    @DisplayName("El documento queda con el tipo en mayúsculas y el número sin puntos")
    void documentos() throws SQLException {
        assertThat(consultar("select tipo_documento, numero_documento from persona_donante"))
                .containsExactly(List.of("DNI", "28456905"));
    }

    @Test
    @DisplayName("Las columnas nuevas arrancan vacías o en cero y el proveedor queda con el nombre del enumerado")
    void columnasNuevas() throws SQLException {
        assertThat(consultar("select version, fecha_baja, proveedor_logistica from donacion"))
                .containsExactly(Arrays.asList(0L, null, "DONATRACK"));
        assertThat(consultar("select version, ultimo_aviso_inactividad from persona_donante"))
                .containsExactly(Arrays.asList(0L, null));
        assertThat(consultar("select count(*) from mensaje_saliente")).containsExactly(List.of(0L));
    }
}
