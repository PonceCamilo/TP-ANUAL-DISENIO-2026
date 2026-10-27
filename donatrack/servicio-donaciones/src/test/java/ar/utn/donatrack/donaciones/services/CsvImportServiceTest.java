package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.importacion.DonanteCsvRowParser;
import ar.utn.donatrack.donaciones.importacion.DonanteFactory;
import ar.utn.donatrack.donaciones.importacion.ImportReport;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.estado.ActivoState;
import org.springframework.transaction.support.TransactionOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la importación masiva de donantes por CSV (requisito de Entrega 1).
 *
 * Reglas que se verifican:
 *   - La PRIMERA línea del archivo es el encabezado y se saltea.
 *   - El EMAIL es la clave de idempotencia: si ya existe un donante con ese
 *     email se ACTUALIZA, si no se CREA. Correr la misma importación dos veces
 *     no duplica donantes.
 *   - Una fila con formato inválido se registra como error y el proceso SIGUE
 *     con las demás: un archivo de miles de filas no puede caerse por una mala.
 *   - El resultado es un ImportReport con el detalle por línea.
 *
 * Se usan el parser y la factory reales (no mocks) porque el valor del test está
 * en verificar el flujo completo de una importación; el repositorio sí se mockea
 * para no depender de almacenamiento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CsvImportService - importación masiva de donantes")
class CsvImportServiceTest {

    private static final String ENCABEZADO = "TipoPersona,TipoDoc,Documento,Nombre,Email,Telefono\n";

    @Mock
    private PersonaDonanteRepositoryInterface donanteRepository;

    @Captor
    private ArgumentCaptor<Collection<PersonaDonante>> loteCaptor;

    private CsvImportService servicio;

    @BeforeEach
    void crearServicio() {
        // Sin transacción real: el repositorio está mockeado. La transacción por
        // lote se prueba contra la base en MedicionConsultasTest.
        servicio = new CsvImportService(donanteRepository, new DonanteCsvRowParser(), new DonanteFactory(),
                TransactionOperations.withoutTransaction());
    }

    /** Convierte el contenido de un CSV en el stream que recibe el servicio. */
    private InputStream csv(String contenido) {
        return new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8));
    }

    /** Donante humano ya existente en el sistema, con el email indicado. */
    private PersonaHumana donanteExistente(String email) {
        return PersonaHumana.builder()
                .id(UUID.randomUUID())
                .nombre("Nombre viejo")
                .apellido("Apellido viejo")
                .tipoDocumento("DNI")
                .numeroDocumento("00000000")
                .email(email)
                .estado(new ActivoState())
                .build();
    }

    @Nested
    @DisplayName("Alta de donantes nuevos")
    class AltaDeDonantes {

        @Test
        @DisplayName("Crea un donante por cada fila válida del archivo")
        void creaLosDonantesDelArchivo() throws IOException {
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,1155667788\n"
                    + "JURIDICA,CUIT,30-11122233-4,Fundación Ejemplo,contacto@fundacion.org,\n";

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.totalCreados()).isEqualTo(2);
            assertThat(reporte.totalActualizados()).isZero();
            assertThat(reporte.tieneErrores()).isFalse();
            assertThat(reporte.totalProcesadas()).isEqualTo(2);
        }

        @Test
        @DisplayName("Los donantes creados se persisten con el tipo correcto según el CSV")
        void persisteElTipoCorrecto() throws IOException {
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n"
                    + "JURIDICA,CUIT,30-11122233-4,Fundación Ejemplo,contacto@fundacion.org,\n";

            servicio.importar(csv(contenido));

            verify(donanteRepository).guardarTodos(loteCaptor.capture());
            List<PersonaDonante> guardadas = List.copyOf(loteCaptor.getValue());
            assertThat(guardadas).hasSize(2);
            assertThat(guardadas.get(0)).isInstanceOf(PersonaHumana.class);
            assertThat(guardadas.get(1)).isInstanceOf(PersonaJuridica.class);
        }

        @Test
        @DisplayName("La primera línea se ignora por ser el encabezado")
        void salteaElEncabezado() throws IOException {
            // Si el encabezado no se salteara, se intentaría crear un donante
            // llamado "Nombre" con email "Email" y el reporte tendría un error.
            String contenido = ENCABEZADO + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n";

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.totalProcesadas()).isEqualTo(1);
            assertThat(reporte.tieneErrores()).isFalse();
        }

        @Test
        @DisplayName("Un archivo que solo tiene el encabezado no crea nada")
        void archivoSoloEncabezado() throws IOException {
            ImportReport reporte = servicio.importar(csv(ENCABEZADO));

            assertThat(reporte.totalProcesadas()).isZero();
            verify(donanteRepository, never()).guardarTodos(anyCollection());
        }
    }

    @Nested
    @DisplayName("Idempotencia por email")
    class IdempotenciaPorEmail {

        @Test
        @DisplayName("Si el email ya existe, el donante se ACTUALIZA en lugar de duplicarse")
        void emailExistenteActualiza() throws IOException {
            // Regla central: reimportar el mismo archivo no debe generar donantes
            // repetidos, solo refrescar sus datos.
            String contenido = ENCABEZADO + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n";
            when(donanteRepository.obtenerPorEmails(anyCollection())).thenReturn(Map.of("juan@example.com", donanteExistente("juan@example.com")));

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.totalActualizados()).isEqualTo(1);
            assertThat(reporte.totalCreados()).isZero();
        }

        @Test
        @DisplayName("Al actualizar se refrescan documento, nombre y apellido")
        void actualizaLosDatosIdentificatorios() throws IOException {
            String contenido = ENCABEZADO + "HUMANA,LC,99999999,Juan Carlos Pérez,juan@example.com,\n";
            PersonaHumana existente = donanteExistente("juan@example.com");
            when(donanteRepository.obtenerPorEmails(anyCollection())).thenReturn(Map.of("juan@example.com", existente));

            servicio.importar(csv(contenido));

            assertThat(existente.getTipoDocumento()).isEqualTo("LC");
            assertThat(existente.getNumeroDocumento()).isEqualTo("99999999");
            assertThat(existente.getNombre()).isEqualTo("Juan");
            assertThat(existente.getApellido()).isEqualTo("Carlos Pérez");
        }

        @Test
        @DisplayName("El email NUNCA se modifica: es la clave que identifica al donante")
        void elEmailNoSeModifica() throws IOException {
            String contenido = ENCABEZADO + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n";
            PersonaHumana existente = donanteExistente("juan@example.com");
            when(donanteRepository.obtenerPorEmails(anyCollection())).thenReturn(Map.of("juan@example.com", existente));

            servicio.importar(csv(contenido));

            assertThat(existente.getEmail()).isEqualTo("juan@example.com");
        }

        @Test
        @DisplayName("Al actualizar una persona jurídica se refresca la razón social")
        void actualizaRazonSocial() throws IOException {
            String contenido = ENCABEZADO + "JURIDICA,CUIT,30-11122233-4,Fundación Nueva,contacto@fundacion.org,\n";
            PersonaJuridica existente = PersonaJuridica.builder()
                    .id(UUID.randomUUID())
                    .razonSocial("Fundación Vieja")
                    .email("contacto@fundacion.org")
                    .estado(new ActivoState())
                    .build();
            when(donanteRepository.obtenerPorEmails(anyCollection())).thenReturn(Map.of("contacto@fundacion.org", existente));

            servicio.importar(csv(contenido));

            assertThat(existente.getRazonSocial()).isEqualTo("Fundación Nueva");
        }

        @Test
        @DisplayName("Un archivo mixto crea los nuevos y actualiza los existentes")
        void archivoMixto() throws IOException {
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n"
                    + "HUMANA,DNI,30333444,Ana Gómez,ana@example.com,\n";
            when(donanteRepository.obtenerPorEmails(anyCollection()))
                    .thenReturn(Map.of("juan@example.com", donanteExistente("juan@example.com")));

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.totalActualizados()).isEqualTo(1);
            assertThat(reporte.totalCreados()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Tolerancia a errores: una fila mala no frena el archivo")
    class ToleranciaAErrores {

        @Test
        @DisplayName("Una fila con tipo de persona inválido se registra como error y el resto se procesa")
        void filaInvalidaNoDetieneElProceso() throws IOException {
            // Este es el comportamiento que hace usable la importación masiva:
            // el administrador recibe el detalle de qué líneas corregir, pero las
            // filas correctas ya quedaron cargadas.
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n"
                    + "EMPRESA,CUIT,30-11122233-4,Fundación Ejemplo,contacto@fundacion.org,\n"
                    + "HUMANA,DNI,30333444,Ana Gómez,ana@example.com,\n";

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.totalCreados()).isEqualTo(2);
            assertThat(reporte.getErrores()).hasSize(1);
            assertThat(reporte.tieneErrores()).isTrue();
        }

        @Test
        @DisplayName("El error del reporte indica el número de línea del archivo original")
        void elErrorIndicaLaLinea() throws IOException {
            // La numeración arranca en 2 porque la línea 1 es el encabezado.
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n"
                    + "HUMANA,DNI,30333444,Ana Gómez,,\n";

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.getErrores()).hasSize(1);
            assertThat(reporte.getErrores().getFirst().getNumeroLinea()).isEqualTo(3);
        }

        @Test
        @DisplayName("Una fila con columnas faltantes se registra como error")
        void columnasFaltantes() throws IOException {
            String contenido = ENCABEZADO + "HUMANA,DNI,30111222\n";

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.getErrores()).hasSize(1);
            verify(donanteRepository, never()).guardarTodos(anyCollection());
        }

        @Test
        @DisplayName("El error del reporte incluye el motivo, para que el administrador sepa qué corregir")
        void elErrorIncluyeElMotivo() throws IOException {
            String contenido = ENCABEZADO + "HUMANA,DNI,,Juan Pérez,juan@example.com,\n";

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.getErrores().getFirst().getDetalle()).isNotBlank();
        }

        @Test
        @DisplayName("Si falla el guardado del lote, reintenta persona por persona y solo marca error en la que falla")
        void errorAlGuardarNoDetieneElProceso() throws IOException {
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n"
                    + "HUMANA,DNI,30333444,Ana Gómez,ana@example.com,\n";
            doThrow(new RuntimeException("Fallo del lote")).when(donanteRepository).guardarTodos(anyCollection());
            doThrow(new RuntimeException("Fallo de persistencia")).when(donanteRepository)
                    .guardar(argThat(persona -> persona != null && "juan@example.com".equals(persona.getEmail())));

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.getErrores()).hasSize(1);
            assertThat(reporte.getErrores().getFirst().getEmail()).isEqualTo("juan@example.com");
            assertThat(reporte.getErrores().getFirst().getDetalle()).contains("Fallo de persistencia");
            assertThat(reporte.totalCreados()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Procesamiento por lotes y datos de contacto (Entrega 5)")
    class LotesYContacto {

        @Test
        @DisplayName("Un lote hace una sola búsqueda por email y un solo guardado")
        void unaBusquedaYUnGuardadoPorLote() throws IOException {
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n"
                    + "HUMANA,DNI,30333444,Ana Gómez,ana@example.com,\n"
                    + "JURIDICA,CUIT,30-11122233-4,Fundación Ejemplo,contacto@fundacion.org,\n";

            servicio.importar(csv(contenido));

            verify(donanteRepository, times(1)).obtenerPorEmails(anyCollection());
            verify(donanteRepository, times(1)).guardarTodos(anyCollection());
            verify(donanteRepository, never()).obtenerPorEmail(anyString());
        }

        @Test
        @DisplayName("Un email repetido en el archivo: la primera fila crea y la segunda actualiza a la misma persona")
        void emailRepetidoEnElArchivo() throws IOException {
            // El CSV de 20.000 filas del enunciado trae 14 emails repetidos.
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n"
                    + "HUMANA,DNI,30111222,Juan Carlos Pérez,Juan@Example.com,\n";

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.totalCreados()).isEqualTo(1);
            assertThat(reporte.totalActualizados()).isEqualTo(1);
            verify(donanteRepository).guardarTodos(loteCaptor.capture());
            assertThat(loteCaptor.getValue()).hasSize(1);
            PersonaHumana unica = (PersonaHumana) loteCaptor.getValue().iterator().next();
            assertThat(unica.getApellido()).isEqualTo("Carlos Pérez");
        }

        @Test
        @DisplayName("Al crear, guarda el teléfono como contacto y el email como medio predeterminado")
        void guardaTelefonoYMedioPredeterminado() throws IOException {
            String contenido = ENCABEZADO + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,+54 11 5555-1111\n";

            servicio.importar(csv(contenido));

            verify(donanteRepository).guardarTodos(loteCaptor.capture());
            PersonaDonante creada = loteCaptor.getValue().iterator().next();
            assertThat(creada.getMedioContactoPredeterminado()).isInstanceOf(Email.class);
            assertThat(creada.getMedioContactoPredeterminado().getValor()).isEqualTo("juan@example.com");
            assertThat(creada.getContactos()).singleElement()
                    .satisfies(contacto -> {
                        assertThat(contacto).isInstanceOf(Telefono.class);
                        assertThat(contacto.getValor()).isEqualTo("+54 11 5555-1111");
                    });
        }

        @Test
        @DisplayName("Al actualizar, reemplaza el teléfono que tenía la persona")
        void actualizaElTelefono() throws IOException {
            PersonaHumana existente = donanteExistente("juan@example.com");
            existente.reemplazarContacto(Telefono.builder().valor("1111-1111").build());
            when(donanteRepository.obtenerPorEmails(anyCollection())).thenReturn(Map.of("juan@example.com", existente));
            String contenido = ENCABEZADO + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,2222-2222\n";

            servicio.importar(csv(contenido));

            assertThat(existente.getContactos()).singleElement()
                    .extracting(contacto -> contacto.getValor()).isEqualTo("2222-2222");
        }
    }

    @Nested
    @DisplayName("Contenido del reporte final")
    class ReporteFinal {

        @Test
        @DisplayName("El total procesado es la suma de creados, actualizados y errores")
        void totalesCoherentes() throws IOException {
            String contenido = ENCABEZADO
                    + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n"
                    + "HUMANA,DNI,30333444,Ana Gómez,ana@example.com,\n"
                    + "EMPRESA,CUIT,30-11122233-4,Fundación,info@fundacion.org,\n";
            when(donanteRepository.obtenerPorEmails(anyCollection()))
                    .thenReturn(Map.of("juan@example.com", donanteExistente("juan@example.com")));

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.totalProcesadas())
                    .isEqualTo(reporte.totalCreados() + reporte.totalActualizados() + reporte.getErrores().size());
        }

        @Test
        @DisplayName("Cada resultado exitoso guarda el email de la fila para poder rastrearla")
        void guardaElEmailDeCadaFila() throws IOException {
            String contenido = ENCABEZADO + "HUMANA,DNI,30111222,Juan Pérez,juan@example.com,\n";

            ImportReport reporte = servicio.importar(csv(contenido));

            assertThat(reporte.getResultados().getFirst().getEmail()).isEqualTo("juan@example.com");
        }
    }
}
