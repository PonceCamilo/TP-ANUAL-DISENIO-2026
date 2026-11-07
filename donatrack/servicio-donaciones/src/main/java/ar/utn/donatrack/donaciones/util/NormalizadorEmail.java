package ar.utn.donatrack.donaciones.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Forma canónica de un email: sin espacios alrededor, en minúsculas y con las
 * letras acentuadas en forma NFC.
 *
 * POR QUÉ (Entrega 5): la unicidad del email la garantiza un UNIQUE de la base,
 * y la base compara bytes. "Ana@Mail.com" y "ana@mail.com" son el mismo email,
 * y una "í" puede escribirse como un carácter o como "i" + acento combinado.
 * Guardar siempre la forma canónica hace que el UNIQUE y el índice sirvan para
 * buscar sin funciones (lower(...)) que impedirían usar el índice.
 *
 * Se usa Locale.ROOT para que el resultado no dependa del idioma del servidor.
 */
public final class NormalizadorEmail {

    private NormalizadorEmail() {
    }

    public static String normalizar(String email) {
        if (email == null) {
            return null;
        }
        return Normalizer.normalize(email.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    /**
     * Forma de un email: una sola arroba, parte local y dominio no vacíos, sin
     * espacios ni puntos al borde o repetidos, y un dominio con al menos un punto
     * y etiquetas que no empiezan ni terminan con guion. Acepta letras con acento
     * en la parte local, que aparecen en los datos reales
     * (benjamíncastro8929@yahoo.com).
     *
     * La importación por CSV la usa para rechazar la fila: antes solo se
     * controlaba que el email no estuviera vacío.
     */
    public static boolean esValido(String email) {
        if (email == null) {
            return false;
        }
        String candidato = email.strip();
        int arroba = candidato.indexOf('@');
        if (arroba <= 0 || arroba != candidato.lastIndexOf('@') || arroba == candidato.length() - 1) {
            return false;
        }
        if (candidato.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        String local = candidato.substring(0, arroba);
        String dominio = candidato.substring(arroba + 1);
        if (local.startsWith(".") || local.endsWith(".") || local.contains("..")) {
            return false;
        }
        String[] etiquetas = dominio.split("\\.", -1);
        if (etiquetas.length < 2 || etiquetas[etiquetas.length - 1].length() < 2) {
            return false;
        }
        for (String etiqueta : etiquetas) {
            if (etiqueta.isEmpty() || etiqueta.startsWith("-") || etiqueta.endsWith("-")) {
                return false;
            }
        }
        return true;
    }
}
