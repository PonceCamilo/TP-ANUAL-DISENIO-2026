package ar.utn.donatrack.donaciones.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Forma canónica de un texto para compararlo: sin espacios en los bordes, con
 * los espacios internos colapsados en uno, sin acentos y en minúsculas con
 * Locale.ROOT (que no depende del idioma del servidor).
 *
 * La usan la subcategoría de las donaciones y el matching contra las
 * necesidades, así "Ropa  de Abrigo", "ropa de abrigo" y "Rópa de abrigo" son lo
 * mismo. Antes cada lugar hacía su propio toLowerCase(), con el locale por
 * defecto y sin quitar acentos.
 */
public final class NormalizadorTexto {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private NormalizadorTexto() {
    }

    /** Devuelve "" para null, así quien compara no tiene que preguntar por null. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = MARCAS_DIACRITICAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return ESPACIOS.matcher(sinAcentos.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }
}
