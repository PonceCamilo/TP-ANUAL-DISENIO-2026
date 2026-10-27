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
}
