package ar.utn.donatrack.donaciones.migraciones;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las migraciones, aplicadas sobre una base vacía, dejan exactamente el esquema
 * que esperan las entidades: el contexto levanta con ddl-auto=validate, que falla
 * si falta una tabla o una columna o si su tipo no coincide.
 *
 * Es lo mismo que pasa al arrancar el servicio contra una base nueva. El resto de
 * los tests de contexto usa create-drop y no corre las migraciones.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:migraciones;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("Migraciones: crean el esquema que validan las entidades")
class MigracionesTest {

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Se aplican todas las versiones en orden y Hibernate valida el esquema resultante")
    void aplicaTodasYValida() {
        MigrationInfo[] aplicadas = flyway.info().applied();

        assertThat(Arrays.stream(aplicadas).map(m -> m.getVersion().getVersion()))
                .containsExactly("1", "2", "3", "4");
        assertThat(flyway.info().pending()).isEmpty();
    }
}
