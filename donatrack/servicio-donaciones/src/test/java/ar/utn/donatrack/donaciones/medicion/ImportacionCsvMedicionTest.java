package ar.utn.donatrack.donaciones.medicion;

import ar.utn.donatrack.donaciones.clientes.BrokerLogisticaClient;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
import ar.utn.donatrack.donaciones.importacion.EstadoImport;
import ar.utn.donatrack.donaciones.importacion.ImportFilaCSV;
import ar.utn.donatrack.donaciones.importacion.ImportReport;
import ar.utn.donatrack.donaciones.services.CsvImportService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mide la importación masiva con un archivo CSV real (por ejemplo, el de 20.000
 * filas del enunciado). No corre en la suite normal: el archivo no se sube al
 * repositorio, así que hay que pasar su ruta.
 *
 *   mvn -pl servicio-donaciones test -Dtest=ImportacionCsvMedicionTest \
 *       -Ddonatrack.csv.medicion=/ruta/al/archivo.csv
 *
 * Importa el archivo dos veces: la primera crea los donantes y la segunda los
 * actualiza a todos (el email es la clave), que son los dos caminos del enunciado.
 * El resultado queda en target/medicion-csv-{etiqueta}.txt.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:medicion_csv;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"
})
@EnabledIfSystemProperty(named = "donatrack.csv.medicion", matches = ".+")
@DisplayName("Medición - importación masiva con un CSV real")
class ImportacionCsvMedicionTest {

    @Autowired private CsvImportService csvImportService;
    @Autowired private EntityManagerFactory emf;

    @MockitoBean private NotificacionClient notificacionClient;
    @MockitoBean private IncentivosClient incentivosClient;
    @MockitoBean private BrokerLogisticaClient brokerLogisticaClient;

    @Test
    @DisplayName("Importa el archivo dos veces (altas y actualizaciones) y registra tiempo y sentencias")
    void importarArchivoReal() throws Exception {
        Path archivo = Path.of(System.getProperty("donatrack.csv.medicion"));
        Statistics estadisticas = emf.unwrap(SessionFactory.class).getStatistics();

        String altas = importar(archivo, estadisticas, "Primera importación");
        String actualizaciones = importar(archivo, estadisticas, "Segunda importación");

        String etiqueta = System.getProperty("donatrack.medicion.etiqueta", "actual");
        String informe = "Medición de importación CSV (" + etiqueta + ")\nArchivo: " + archivo.getFileName()
                + "\n\n" + altas + "\n" + actualizaciones;
        System.out.println(informe);
        Path destino = Path.of("target", "medicion-csv-" + etiqueta + ".txt");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, informe, StandardCharsets.UTF_8);
    }

    private String importar(Path archivo, Statistics estadisticas, String nombre) throws Exception {
        estadisticas.clear();
        long inicio = System.nanoTime();
        ImportReport reporte;
        try (InputStream entrada = Files.newInputStream(archivo)) {
            reporte = csvImportService.importar(entrada);
        }
        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        long sentencias = estadisticas.getPrepareStatementCount();

        Map<EstadoImport, Long> porEstado = reporte.getResultados().stream()
                .collect(Collectors.groupingBy(ImportFilaCSV::getEstado, Collectors.counting()));
        assertThat(reporte.getResultados()).isNotEmpty();

        long filas = reporte.getResultados().size();
        return String.format("%s: %d filas en %d ms (%.2f ms por fila), %d sentencias SQL (%.1f por fila). Resultado: %s%n",
                nombre, filas, milisegundos, (double) milisegundos / filas, sentencias,
                (double) sentencias / filas, porEstado.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue, Long::sum,
                                TreeMap::new)));
    }
}
