package ar.utn.donatrack.donaciones.config;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.services.AsignacionBatchService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.util.ArrayList;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bloqueo de las tareas programadas contra una base real (H2 con las
 * migraciones, que crean la tabla shedlock): lo que pasa cuando otra instancia
 * del servicio ya está corriendo la misma tarea.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:bloqueo_tareas;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "donaciones.procesos.bloqueo-distribuido=true"
})
@DisplayName("Bloqueo de tareas: con varias instancias, cada tarea corre en una sola")
class BloqueoDeTareasTest {

    @Autowired private LockProvider bloqueos;
    @Autowired private AsignacionBatchService matchmaking;
    @Autowired private RepositorioCandidatosAsignacion candidatos;
    @Autowired private TransactionOperations transaccion;

    /** Lo que hace otra instancia al tomar la tarea. */
    private Optional<SimpleLock> tomar(String tarea) {
        return bloqueos.lock(new LockConfiguration(Instant.now(), tarea, Duration.ofMinutes(5), Duration.ZERO));
    }

    @Test
    @DisplayName("Mientras una instancia tiene la tarea, otra no la consigue; al soltarla, sí")
    void unaSolaInstanciaALaVez() {
        Optional<SimpleLock> primera = tomar("tarea-de-prueba");
        assertThat(primera).isPresent();
        assertThat(tomar("tarea-de-prueba")).isEmpty();

        primera.get().unlock();

        Optional<SimpleLock> otraVez = tomar("tarea-de-prueba");
        assertThat(otraVez).isPresent();
        otraVez.get().unlock();
    }

    @Test
    @DisplayName("Si otra instancia está corriendo el matchmaking, este no corre y la bandeja no se toca")
    void elMatchmakingNoCorreDosVeces() {
        UUID idDonacion = UUID.randomUUID();
        transaccion.executeWithoutResult(estado -> candidatos.reemplazarTodos(List.of(
                RankingPrecalculado.de(idDonacion, new ResultadoMatchmaking(Map.of()), LocalDateTime.now()))));

        SimpleLock otraInstancia = tomar("matchmaking-nocturno").orElseThrow();
        matchmaking.ejecutarMatchmakingNocturno();
        assertThat(candidatos.obtenerTodos()).extracting(RankingPrecalculado::getIdDonacion).containsExactly(idDonacion);

        otraInstancia.unlock();
        // Sin donaciones en depósito, la corrida deja la bandeja vacía.
        matchmaking.ejecutarMatchmakingNocturno();
        assertThat(candidatos.obtenerTodos()).isEmpty();
    }

    @Test
    @DisplayName("Toda tarea programada del servicio tiene su bloqueo")
    void todasLasTareasTienenBloqueo() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider componentes = new ClassPathScanningCandidateComponentProvider(false);
        componentes.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        List<Method> tareas = new ArrayList<>();
        for (var definicion : componentes.findCandidateComponents("ar.utn.donatrack.donaciones")) {
            Class<?> clase = Class.forName(definicion.getBeanClassName());
            Arrays.stream(clase.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Scheduled.class)).forEach(tareas::add);
        }

        assertThat(tareas).hasSizeGreaterThanOrEqualTo(6)
                .allSatisfy(tarea -> assertThat(tarea.isAnnotationPresent(SchedulerLock.class))
                        .as(tarea.getDeclaringClass().getSimpleName() + "." + tarea.getName()).isTrue());
    }
}
