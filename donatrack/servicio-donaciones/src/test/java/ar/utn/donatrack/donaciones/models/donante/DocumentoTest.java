package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.exceptions.personasExceptions.DocumentoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Documento - tipo y número validados juntos")
class DocumentoTest {

    @Test
    @DisplayName("Guarda el número normalizado: sin puntos, guiones ni espacios")
    void normaliza() {
        assertThat(Documento.de("dni", "28.456.905")).isEqualTo(Documento.de("DNI", "28456905"));
        assertThat(Documento.de("CUIT", "30-11122233-5").getNumero()).isEqualTo("30111222335");
        assertThat(Documento.de(" cuit ", "30 11122233 5").getTipo()).isEqualTo("CUIT");
    }

    @ParameterizedTest(name = "{0} {1} es válido")
    @CsvSource({
            "DNI, 1234567", "DNI, 28456905",
            "LC, 123456", "LE, 12345678",
            "CUIT, 30-11122233-5", "CUIT, 20-12345678-6", "CUIL, 27-33444555-6",
            "PASAPORTE, AAA123456"
    })
    void validos(String tipo, String numero) {
        assertThat(Documento.de(tipo, numero).getTipo()).isEqualTo(tipo);
    }

    @ParameterizedTest(name = "{0} {1} no es válido")
    @CsvSource({
            "DNI, 123456", "DNI, 123456789", "DNI, 12A45678",
            "LC, 12345",
            "CUIT, 30-11122233-4", "CUIT, 3011122233", "CUIL, 27-33444555-3",
            "PASAPORTE, AB12"
    })
    void invalidos(String tipo, String numero) {
        assertThatThrownBy(() -> Documento.de(tipo, numero))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining(tipo);
    }

    @Test
    @DisplayName("Un tipo de documento desconocido se rechaza")
    void tipoDesconocido() {
        assertThatThrownBy(() -> Documento.de("CEDULA", "12345678"))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining("CEDULA");
    }

    @Test
    @DisplayName("Una organización se identifica con CUIT")
    void organizacionConCuit() {
        assertThat(Documento.deOrganizacion("CUIT", "30-11122233-5").getTipo()).isEqualTo("CUIT");
        assertThatThrownBy(() -> Documento.deOrganizacion("DNI", "28456905"))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining("CUIT");
    }

    @Test
    @DisplayName("El dígito verificador del CUIT: un dígito cambiado no pasa")
    void digitoVerificador() {
        assertThat(TipoDocumento.CUIT.esValido("30111222335")).isTrue();
        assertThat(TipoDocumento.CUIT.esValido("30111222325")).isFalse();
    }
}
