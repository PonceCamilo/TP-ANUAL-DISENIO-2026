package ar.utn.donatrack.incentivos;

import ar.utn.donatrack.incentivos.integracion.notificaciones.NotificacionTransport;
import ar.utn.donatrack.incentivos.integracion.notificaciones.NotificacionTransportRest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el contexto de Spring cargue y que las tareas calendarizadas
 * queden registradas.
 *
 * POR QUÉ EXISTE: hasta la Entrega 4 este servicio NO ARRANCABA. Tenía
 * spring-boot-starter-data-jpa y 15 clases @Entity, pero ningún driver de base
 * de datos ni properties de datasource, así que Spring fallaba con "Failed to
 * configure a DataSource". Nadie lo detectó porque todos los tests eran
 * unitarios con mocks y ninguno levantaba el contexto.
 *
 * Además faltaba @EnableScheduling, así que los dos @Scheduled de IncentivosJob
 * nunca corrían: ni la revisión de rachas (que implementa la pérdida de
 * misiones, ítem 6 del checklist de la cátedra) ni el cierre del ranking
 * mensual. Spring ignora @Scheduled EN SILENCIO cuando falta esa anotación.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Carga del contexto y tareas calendarizadas")
class ContextoCargaTest {

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @Autowired
    private NotificacionTransport transporte;

    /** Nombres de los métodos @Scheduled que Spring registró al levantar. */
    private String tareasRegistradas() {
        return scheduledTaskHolder.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .map(tarea -> tarea.getRunnable().toString())
                .collect(Collectors.joining(" | "));
    }

    @Test
    @DisplayName("El contexto levanta sin errores")
    void elContextoCarga() {
        assertThat(contexto).isNotNull();
    }

    @Test
    @DisplayName("Hay tareas programadas (prueba que @EnableScheduling está presente)")
    void hayTareasProgramadas() {
        assertThat(scheduledTaskHolder.getScheduledTasks())
                .as("Si está vacío, falta @EnableScheduling en ServicioIncentivosApp")
                .isNotEmpty();
    }

    @Test
    @DisplayName("La revisión de rachas está programada")
    void revisionDeRachasProgramada() {
        // Implementa la pérdida de misiones: la racha se pierde si no se dona
        // durante un mes completo. Es el ítem 6 del checklist de la cátedra.
        assertThat(tareasRegistradas()).contains("revisarRachasCadaTreintaDias");
    }

    @Test
    @DisplayName("El cálculo del ranking mensual está programado")
    void rankingMensualProgramado() {
        assertThat(tareasRegistradas()).contains("calcularRankingMensual");
    }

    @Test
    @DisplayName("Por defecto se usa el transporte REST, sin necesitar RabbitMQ")
    void transportePorDefectoEsRest() {
        // En Docker el compose lo pone en amqp; suelto desde el IDE tiene que
        // poder arrancar sin un broker levantado.
        assertThat(transporte).isInstanceOf(NotificacionTransportRest.class);
    }
}
