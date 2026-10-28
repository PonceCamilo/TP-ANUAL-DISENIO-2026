package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.exceptions.personasExceptions.DocumentoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;

/**
 * Documento de una persona donante: tipo y número, validados juntos.
 *
 * Antes eran dos String sueltos y nadie revisaba que el número tuviera sentido
 * para el tipo (el CSV aceptaba cualquier cosa no vacía). Ahora un Documento solo
 * se crea válido: el número se guarda normalizado (sin puntos, guiones ni
 * espacios) y debe cumplir la forma de su tipo (ver TipoDocumento).
 *
 * MAPEO: @Embeddable en las mismas columnas de antes (tipo_documento y
 * numero_documento): no cambia el esquema.
 */
@Embeddable
@Getter
@EqualsAndHashCode
public class Documento {

    @Column(name = "tipo_documento")
    private String tipo;

    @Column(name = "numero_documento")
    private String numero;

    /** Requerido por JPA. No usar desde el dominio. */
    protected Documento() {
    }

    private Documento(TipoDocumento tipo, String numero) {
        this.tipo = tipo.name();
        this.numero = numero;
    }

    /** Lanza DocumentoInvalidoException (400) si el tipo no existe o el número no corresponde. */
    public static Documento de(String tipo, String numero) {
        TipoDocumento tipoDocumento = TipoDocumento.desde(tipo).orElseThrow(() -> new DocumentoInvalidoException(
                "tipo de documento desconocido '" + tipo + "'. Valores aceptados: "
                        + Arrays.toString(TipoDocumento.values())));
        String normalizado = tipoDocumento.normalizar(numero);
        if (!tipoDocumento.esValido(normalizado)) {
            throw new DocumentoInvalidoException("el " + tipoDocumento + " '" + numero + "' no es válido: debe ser "
                    + tipoDocumento.descripcion() + ".");
        }
        return new Documento(tipoDocumento, normalizado);
    }

    /** Una organización se identifica con su CUIT. */
    public static Documento deOrganizacion(String tipo, String numero) {
        Documento documento = de(tipo, numero);
        if (!TipoDocumento.CUIT.name().equals(documento.tipo)) {
            throw new DocumentoInvalidoException("una persona jurídica se identifica con CUIT, no con " + documento.tipo + ".");
        }
        return documento;
    }

    @Override
    public String toString() {
        return tipo + " " + numero;
    }
}
