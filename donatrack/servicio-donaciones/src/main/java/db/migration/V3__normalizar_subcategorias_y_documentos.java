package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Lleva los datos guardados antes de la revisión a las normalizaciones nuevas:
 *
 *   - subcategoria_tipo_normalizado de donaciones y bienes: antes era el texto en
 *     minúsculas y sin espacios en los extremos; ahora además sin acentos y con
 *     los espacios internos colapsados. Sin recalcularlo, "Azúcar" guardado antes
 *     no coincidiría con una donación nueva de "azucar" (ni con su índice).
 *   - tipo y número de documento de las personas donantes: ahora el tipo va en
 *     mayúsculas y el número sin puntos, guiones ni espacios ("28.456.905" pasa a
 *     "28456905").
 *
 * Es una migración en Java porque las dos cosas son difíciles de escribir igual
 * en SQL para PostgreSQL y para H2 (quitar acentos, reemplazos con expresiones
 * regulares). Las reglas se copian acá a propósito en lugar de llamar a
 * NormalizadorTexto o TipoDocumento: una migración tiene que dar siempre el mismo
 * resultado, aunque esas clases cambien después.
 */
public class V3__normalizar_subcategorias_y_documentos extends BaseJavaMigration {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    private static final Pattern SEPARADORES_DE_DOCUMENTO = Pattern.compile("[\\s.\\-]");

    @Override
    public void migrate(Context context) throws Exception {
        Connection conexion = context.getConnection();
        normalizarSubcategorias(conexion, "donacion");
        normalizarSubcategorias(conexion, "bien");
        normalizarDocumentos(conexion);
    }

    private static void normalizarSubcategorias(Connection conexion, String tabla) throws SQLException {
        Map<Object, String> cambios = new LinkedHashMap<>();
        try (Statement consulta = conexion.createStatement();
             ResultSet filas = consulta.executeQuery("select id, subcategoria_tipo, subcategoria_tipo_normalizado from "
                     + tabla + " where subcategoria_tipo is not null")) {
            while (filas.next()) {
                String normalizado = normalizarTexto(filas.getString(2));
                if (!normalizado.equals(filas.getString(3))) {
                    cambios.put(filas.getObject(1), normalizado);
                }
            }
        }
        try (PreparedStatement actualizar = conexion.prepareStatement(
                "update " + tabla + " set subcategoria_tipo_normalizado = ? where id = ?")) {
            for (Map.Entry<Object, String> cambio : cambios.entrySet()) {
                actualizar.setString(1, cambio.getValue());
                actualizar.setObject(2, cambio.getKey());
                actualizar.addBatch();
            }
            actualizar.executeBatch();
        }
    }

    private static void normalizarDocumentos(Connection conexion) throws SQLException {
        Map<Object, String[]> cambios = new LinkedHashMap<>();
        try (Statement consulta = conexion.createStatement();
             ResultSet filas = consulta.executeQuery(
                     "select id, tipo_documento, numero_documento from persona_donante"
                             + " where tipo_documento is not null or numero_documento is not null")) {
            while (filas.next()) {
                String tipo = filas.getString(2);
                String numero = filas.getString(3);
                String tipoNuevo = tipo == null ? null : tipo.strip().toUpperCase(Locale.ROOT);
                String numeroNuevo = numero == null ? null
                        : SEPARADORES_DE_DOCUMENTO.matcher(numero).replaceAll("").toUpperCase(Locale.ROOT);
                if (!Objects.equals(tipo, tipoNuevo) || !Objects.equals(numero, numeroNuevo)) {
                    cambios.put(filas.getObject(1), new String[]{tipoNuevo, numeroNuevo});
                }
            }
        }
        try (PreparedStatement actualizar = conexion.prepareStatement(
                "update persona_donante set tipo_documento = ?, numero_documento = ? where id = ?")) {
            for (Map.Entry<Object, String[]> cambio : cambios.entrySet()) {
                actualizar.setString(1, cambio.getValue()[0]);
                actualizar.setString(2, cambio.getValue()[1]);
                actualizar.setObject(3, cambio.getKey());
                actualizar.addBatch();
            }
            actualizar.executeBatch();
        }
    }

    /** Minúsculas, sin acentos y con los espacios colapsados (la regla de la revisión). */
    private static String normalizarTexto(String texto) {
        String sinAcentos = MARCAS_DIACRITICAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return ESPACIOS.matcher(sinAcentos.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }
}
