package ar.utn.donatrack.donaciones.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Bloqueo de las tareas programadas (Entrega 5).
 *
 * Con dos instancias del servicio, cada @Scheduled corría en las dos: el
 * matchmaking reemplazaba la bandeja dos veces, el despacho mandaba el mismo
 * lote dos veces al broker y el relay enviaba dos veces el mismo aviso. Cada
 * tarea anotada con @SchedulerLock toma antes una fila de la tabla shedlock; la
 * instancia que no la consigue no la corre esa vez.
 *
 * El bloqueo envuelve a la transacción de la tarea (order más alto que el de
 * @Transactional): se libera recién después del commit, así otra instancia no
 * arranca antes de que los cambios estén confirmados. La hora es la de la base
 * (usingDbTime), no la de cada servidor.
 */
@Configuration
@ConditionalOnProperty(name = "donaciones.procesos.bloqueo-distribuido", havingValue = "true", matchIfMissing = true)
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M", order = Ordered.HIGHEST_PRECEDENCE)
public class BloqueoDeTareasConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }
}
