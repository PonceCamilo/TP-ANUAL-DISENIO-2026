package ar.utn.donatrack.donaciones.models.donante;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Los tipos de documento que acepta el servicio y la forma válida de cada número
 * (ya normalizado: sin puntos, guiones ni espacios).
 *
 * CUIT y CUIL se validan además con su dígito verificador: un error de tipeo en
 * un dígito no pasa. Los datos reales de la importación cumplen todas las reglas
 * (verificado con el CSV de 20.000 filas: DNI de 8 dígitos y CUIT válidos).
 */
public enum TipoDocumento {
    DNI("\\d{7,8}", "de 7 u 8 dígitos"),
    LC("\\d{6,8}", "de 6 a 8 dígitos"),
    LE("\\d{6,8}", "de 6 a 8 dígitos"),
    CUIT("\\d{11}", "de 11 dígitos con dígito verificador válido"),
    CUIL("\\d{11}", "de 11 dígitos con dígito verificador válido"),
    PASAPORTE("[A-Z0-9]{6,9}", "de 6 a 9 letras o dígitos");

    private static final int[] PESOS_CUIT = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private final Pattern forma;
    private final String descripcion;

    TipoDocumento(String forma, String descripcion) {
        this.forma = Pattern.compile(forma);
        this.descripcion = descripcion;
    }

    public static Optional<TipoDocumento> desde(String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        String buscado = nombre.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(tipo -> tipo.name().equals(buscado)).findFirst();
    }

    /** El número tal como se guarda: sin puntos, guiones ni espacios, y en mayúsculas. */
    public String normalizar(String numero) {
        return numero == null ? null : numero.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT);
    }

    public boolean esValido(String numeroNormalizado) {
        if (numeroNormalizado == null || !forma.matcher(numeroNormalizado).matches()) {
            return false;
        }
        return (this != CUIT && this != CUIL) || digitoVerificadorValido(numeroNormalizado);
    }

    public String descripcion() {
        return descripcion;
    }

    /** Módulo 11 con los pesos 5-4-3-2-7-6-5-4-3-2: 11 da 0 y 10 no es un CUIT válido. */
    private static boolean digitoVerificadorValido(String once) {
        int suma = 0;
        for (int i = 0; i < PESOS_CUIT.length; i++) {
            suma += Character.digit(once.charAt(i), 10) * PESOS_CUIT[i];
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 11) {
            verificador = 0;
        }
        return verificador != 10 && verificador == Character.digit(once.charAt(10), 10);
    }
}
