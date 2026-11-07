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
        assertThat(transporte).isInstanceOf(NotificacionTransportRest.class);
    }
}
