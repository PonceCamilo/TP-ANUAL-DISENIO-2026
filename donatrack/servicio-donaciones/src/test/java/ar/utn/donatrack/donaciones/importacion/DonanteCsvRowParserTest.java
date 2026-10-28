package ar.utn.donatrack.donaciones.importacion;

import ar.utn.donatrack.donaciones.exceptions.csvExceptions.CsvFormatoException;
import ar.utn.donatrack.donaciones.importacion.dto.DonanteImportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del parser de filas del CSV de importación masiva de donantes.
 *
 * Formato esperado del archivo (una fila por donante):
 *   TipoPersona, TipoDoc, Documento, Nombre/RazonSocial, Email, Teléfono(opcional)
 *
 * El parser valida SOLO lo que el CSV puede aportar; los campos que el archivo
 * no trae (edad, género, dirección, rubro) los completa DonanteFactory con
 * valores neutros. Cada problema de formato tiene su propia excepción para que
 * el reporte final le diga al administrador exactamente qué corregir y en qué línea.
 */
@DisplayName("DonanteCsvRowParser - parseo y validación de una fila del CSV")
class DonanteCsvRowParserTest {

    private static final int LINEA = 7;

    private DonanteCsvRowParser parser;

    @BeforeEach
    void crearParser() {
        parser = new DonanteCsvRowParser();
    }

    /** Fila válida de persona humana, usada como base en varios tests. */
    private String[] filaHumanaValida() {
        return new String[]{"HUMANA", "DNI", "30111222", "Juan Pérez", "juan@example.com", "1155667788"};
    }

    @Nested
    @DisplayName("Filas válidas")
    class FilasValidas {

        @Test
        @DisplayName("Parsea una fila completa de persona humana")
        void parseaPersonaHumana() {
            DonanteImportDto dto = parser.parsear(filaHumanaValida(), LINEA);

            assertThat(dto.tipoPersona()).isEqualTo("HUMANA");
            assertThat(dto.tipoDoc()).isEqualTo("DNI");
            assertThat(dto.documento()).isEqualTo("30111222");
            assertThat(dto.nombreORazonSocial()).isEqualTo("Juan Pérez");
            assertThat(dto.email()).isEqualTo("juan@example.com");
            assertThat(dto.telefono()).isEqualTo("1155667788");
        }

        @Test
        @DisplayName("Parsea una fila de persona jurídica")
        void parseaPersonaJuridica() {
            String[] fila = {"JURIDICA", "CUIT", "30-11122233-5", "Fundación Ejemplo", "contacto@fundacion.org", ""};

            DonanteImportDto dto = parser.parsear(fila, LINEA);

            assertThat(dto.tipoPersona()).isEqualTo("JURIDICA");
            assertThat(dto.nombreORazonSocial()).isEqualTo("Fundación Ejemplo");
        }

        @Test
        @DisplayName("El teléfono es opcional: una fila de 5 columnas es válida")
        void telefonoOpcional() {
            String[] sinTelefono = {"HUMANA", "DNI", "30111222", "Juan Pérez", "juan@example.com"};

            DonanteImportDto dto = parser.parsear(sinTelefono, LINEA);

            assertThat(dto.telefono()).isNull();
        }

        @Test
        @DisplayName("Un teléfono en blanco se normaliza a null en lugar de guardarse vacío")
        void telefonoEnBlancoSeNormalizaANull() {
            // Evita persistir cadenas vacías que después parecerían un contacto válido.
            String[] fila = {"HUMANA", "DNI", "30111222", "Juan Pérez", "juan@example.com", "   "};

            assertThat(parser.parsear(fila, LINEA).telefono()).isNull();
        }

        @Test
        @DisplayName("El tipo de persona y el tipo de documento se normalizan a mayúsculas")
        void normalizaAMayusculas() {
            // Tolerancia a cómo el administrador arme el archivo: "humana" y
            // "HUMANA" deben tratarse igual.
            String[] fila = {"humana", "dni", "30111222", "Juan Pérez", "juan@example.com"};

            DonanteImportDto dto = parser.parsear(fila, LINEA);

            assertThat(dto.tipoPersona()).isEqualTo("HUMANA");
            assertThat(dto.tipoDoc()).isEqualTo("DNI");
        }

        @Test
        @DisplayName("Los espacios sobrantes alrededor de cada valor se recortan")
        void recortaEspacios() {
            String[] fila = {"  HUMANA ", " DNI ", " 30111222 ", "  Juan Pérez ", " juan@example.com "};

            DonanteImportDto dto = parser.parsear(fila, LINEA);

            assertThat(dto.documento()).isEqualTo("30111222");
            assertThat(dto.nombreORazonSocial()).isEqualTo("Juan Pérez");
            assertThat(dto.email()).isEqualTo("juan@example.com");
        }

        @Test
        @DisplayName("Las columnas extra después del teléfono se ignoran")
        void columnasExtraSeIgnoran() {
            // Robustez: un archivo con columnas adicionales no debe romper la importación.
            String[] fila = {"HUMANA", "DNI", "30111222", "Juan Pérez", "juan@example.com", "1155667788", "extra", "otra"};

            assertThat(parser.parsear(fila, LINEA).telefono()).isEqualTo("1155667788");
        }
    }

    @Nested
    @DisplayName("Filas inválidas - cada problema tiene su propia excepción")
    class FilasInvalidas {

        @Test
        @DisplayName("Menos de 5 columnas lanza CsvFormatoLineaException indicando la línea")
        void faltanColumnas() {
            String[] incompleta = {"HUMANA", "DNI", "30111222"};

            assertThatThrownBy(() -> parser.parsear(incompleta, LINEA))
                    .isInstanceOf(CsvFormatoException.class)
                    .hasMessageContaining(String.valueOf(LINEA));
        }

        @Test
        @DisplayName("Una fila sin columnas lanza CsvFormatoLineaException")
        void filaVacia() {
            assertThatThrownBy(() -> parser.parsear(new String[]{}, LINEA))
                    .isInstanceOf(CsvFormatoException.class);
        }

        @Test
        @DisplayName("Un tipo de persona distinto de HUMANA/JURIDICA lanza CsvFormatoPersonaException")
        void tipoDePersonaInvalido() {
            // El sistema solo modela estos dos tipos de donante.
            String[] fila = {"EMPRESA", "CUIT", "30111222", "Fundación", "contacto@fundacion.org"};

            assertThatThrownBy(() -> parser.parsear(fila, LINEA))
                    .isInstanceOf(CsvFormatoException.class);
        }

        @Test
        @DisplayName("Un email en blanco lanza CsvFormatoMailException")
        void emailEnBlanco() {
            // El email es obligatorio: es el canal de contacto y la clave que
            // decide si la fila crea un donante nuevo o actualiza uno existente.
            String[] fila = {"HUMANA", "DNI", "30111222", "Juan Pérez", "   "};

            assertThatThrownBy(() -> parser.parsear(fila, LINEA))
                    .isInstanceOf(CsvFormatoException.class);
        }

        @Test
        @DisplayName("Un email con formato inválido se rechaza (antes solo se pedía que no estuviera vacío)")
        void emailInvalido() {
            String[] fila = {"HUMANA", "DNI", "30111222", "Juan Pérez", "juan@mail", ""};

            assertThatThrownBy(() -> parser.parsear(fila, 7))
                    .isInstanceOf(CsvFormatoException.class)
                    .hasMessageContaining("Línea 7")
                    .hasMessageContaining("juan@mail");
        }

        @Test
        @DisplayName("Un documento que no corresponde a su tipo se rechaza (DNI de 3 dígitos, CUIT mal verificado)")
        void documentoInvalido() {
            assertThatThrownBy(() -> parser.parsear(
                    new String[]{"HUMANA", "DNI", "123", "Juan Pérez", "juan@mail.com", ""}, 4))
                    .isInstanceOf(CsvFormatoException.class)
                    .hasMessageContaining("Línea 4")
                    .hasMessageContaining("DNI");
            assertThatThrownBy(() -> parser.parsear(
                    new String[]{"JURIDICA", "CUIT", "30-11122233-4", "Fundación", "info@fundacion.org", ""}, 5))
                    .isInstanceOf(CsvFormatoException.class)
                    .hasMessageContaining("CUIT");
        }

        @Test
        @DisplayName("Una persona jurídica tiene que venir con CUIT")
        void juridicaSinCuit() {
            String[] fila = {"JURIDICA", "DNI", "30111222", "Fundación", "info@fundacion.org", ""};

            assertThatThrownBy(() -> parser.parsear(fila, 9))
                    .isInstanceOf(CsvFormatoException.class)
                    .hasMessageContaining("CUIT");
        }

        @Test
        @DisplayName("El documento queda normalizado en la fila parseada")
        void documentoNormalizado() {
            DonanteImportDto dto = parser.parsear(
                    new String[]{"HUMANA", "dni", "30.111.222", "Juan Pérez", "juan@mail.com", ""}, 2);

            assertThat(dto.tipoDoc()).isEqualTo("DNI");
            assertThat(dto.documento()).isEqualTo("30111222");
        }

        @Test
        @DisplayName("Un documento en blanco lanza CsvFormatoDocumentoException")
        void documentoEnBlanco() {
            String[] fila = {"HUMANA", "DNI", "  ", "Juan Pérez", "juan@example.com"};

            assertThatThrownBy(() -> parser.parsear(fila, LINEA))
                    .isInstanceOf(CsvFormatoException.class);
        }

        @Test
        @DisplayName("Un nombre en blanco lanza CsvFormatoNombreException")
        void nombreEnBlanco() {
            String[] fila = {"HUMANA", "DNI", "30111222", "   ", "juan@example.com"};

            assertThatThrownBy(() -> parser.parsear(fila, LINEA))
                    .isInstanceOf(CsvFormatoException.class);
        }

        @Test
        @DisplayName("Cuando hay varios problemas, se reporta el primero según el orden de validación")
        void ordenDeValidacion() {
            // El orden es: tipo de persona, email, documento, nombre. Esta fila
            // tiene mal el tipo Y el email, y debe fallar por el tipo.
            String[] fila = {"EMPRESA", "DNI", "30111222", "Juan Pérez", ""};

            assertThatThrownBy(() -> parser.parsear(fila, LINEA))
                    .isInstanceOf(CsvFormatoException.class);
        }
    }
}
