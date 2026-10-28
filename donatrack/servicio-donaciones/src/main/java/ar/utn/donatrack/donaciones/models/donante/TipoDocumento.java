package ar.utn.donatrack.donaciones.models.donante;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Los tipos de documento que acepta el servicio y la forma válida de cada número
 * (ya normalizado: sin puntos, guiones ni espacios).
 *
 * CUIT y CUIL se validan por su forma (11 dígitos) y no por el dígito
 * verificador. El CSV de 20.000 filas del enunciado trae 580 CUIT (de 6.055
 * personas jurídicas) cuyo verificador no cierra, y el CUIT de ejemplo del
 * propio enunciado (30-12345678-9) tampoco: con el verificador, esas personas no
 * se podían importar ni dar de alta. Que un CUIT exista de verdad lo dice AFIP,
 * no la forma del número.
 */
public enum TipoDocumento {
    DNI("\\d{7,8}", "de 7 u 8 dígitos"),
    LC("\\d{6,8}", "de 6 a 8 dígitos"),
    LE("\\d{6,8}", "de 6 a 8 dígitos"),
    CUIT("\\d{11}", "de 11 dígitos"),
    CUIL("\\d{11}", "de 11 dígitos"),
    PASAPORTE("[A-Z0-9]{6,9}", "de 6 a 9 letras o dígitos");

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
        return numeroNormalizado != null && forma.matcher(numeroNormalizado).matches();
    }

    public String descripcion() {
        return descripcion;
    }
}
