package ar.utn.donatrack.donaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que las tareas calendarizadas estén efectivamente registradas.
 *
 * POR QUÉ EXISTE ESTE TEST: sin @EnableScheduling en ServicioDonacionesApp,
 * Spring ignora las anotaciones @Scheduled EN SILENCIO. El código compila, los
 * tests unitarios de cada service pasan (porque invocan el método directamente)
 * y la aplicación arranca sin un solo warning, pero los procesos nunca corren.
 * Fue exactamente lo que pasaba acá: ni la notificación a donantes inactivos ni
 * el matchmaking nocturno se ejecutaban en producción.
 *
 * Este es el único test que lo detecta, porque es el único que levanta el
 * contexto de Spring y le pregunta qué tareas quedaron programadas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Scheduling - las tareas calendarizadas quedan registradas al arrancar")
class SchedulingHabilitadoTest {

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    /** Nombres de los métodos @Scheduled que Spring registró al levantar el contexto. */
    private String tareasRegistradas() {
        Set<ScheduledTask> tareas = scheduledTaskHolder.getScheduledTasks();
        return tareas.stream()
                .map(tarea -> tarea.getTask().getRunnable().toString())
                .collect(Collectors.joining(" | "));
    }

    @Test
    @DisplayName("Hay al menos una tarea programada (prueba que @EnableScheduling está presente)")
    void hayTareasProgramadas() {
        assertThat(scheduledTaskHolder.getScheduledTasks())
                .as("Si está vacío, falta @EnableScheduling en ServicioDonacionesApp")
                .isNotEmpty();
    }

    @Test
    @DisplayName("La notificación diaria a donantes inactivos está programada")
    void inactividadDonantesProgramada() {
        // Requisito de Entrega 2: avisar al donante que lleva más de 20 días sin
        // interactuar para incentivarlo a volver a donar.
        assertThat(tareasRegistradas()).contains("notificarDonantesInactivos");
    }

    @Test
    @DisplayName("El matchmaking nocturno de las donaciones en depósito está programado")
    void matchmakingNocturnoProgramado() {
        // Requisito de Entrega 2: ejecutar los algoritmos de asignación sobre las
        // donaciones EN_DEPOSITO en horarios de baja carga.
        assertThat(tareasRegistradas()).contains("ejecutarMatchmakingNocturno");
    }

    @Test
    @DisplayName("La planificación de entregas del día siguiente está programada")
    void planificacionEntregasProgramada() {
        // Requisito de Entrega 3: generar los planes de ruta para la siguiente
        // jornada operativa en horarios de baja carga. Es lo que dispara el
        // broker de logística que pide la Entrega 4.
        assertThat(tareasRegistradas()).contains("planificarEntregasDelDiaSiguiente");
    }
}
