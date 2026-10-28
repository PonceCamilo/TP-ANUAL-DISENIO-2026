package ar.utn.donatrack.donaciones.integracion;

import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioRecepcionesEntidad;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donante.Documento;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionOperations;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El servicio contra PostgreSQL 16, la base de producción, en un contenedor:
 * lo que H2 en modo PostgreSQL puede no reproducir igual. Las migraciones
 * crean el esquema y Hibernate lo valida; después se prueban el código de error
 * de la clave duplicada (lo usa el 409 del handler), las sentencias nativas y el
 * bloqueo de tareas con la hora de la base. Sin Docker disponible, se saltea.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "donaciones.procesos.bloqueo-distribuido=true"
})
@DisplayName("PostgreSQL 16: migraciones, clave duplicada, sentencias nativas y bloqueo de tareas")
class PostgresIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private Flyway flyway;
    @Autowired private RepositorioPersonasDonantes personas;
    @Autowired private RepositorioDonaciones donaciones;
    @Autowired private RepositorioRecepcionesEntidad recepciones;
    @Autowired private LockProvider bloqueos;
    @Autowired private TransactionOperations transaccion;

    private static PersonaHumana persona(String email) {
        return PersonaHumana.builder().nombre("Ana").email(email).documento(Documento.de("DNI", "30111222")).build();
    }

    @Test
    @DisplayName("Las migraciones dejan el esquema que valida Hibernate")
    void migraciones() {
        assertThat(Arrays.stream(flyway.info().applied()).map(m -> m.getVersion().getVersion()))
                .containsExactly("1", "2", "3", "4");
    }

    @Test
    @DisplayName("Un email repetido viola el UNIQUE con el estado SQL 23505, el que el handler responde como 409")
    void emailDuplicado() {
        String email = "repetido" + UUID.randomUUID() + "@mail.com";
        transaccion.executeWithoutResult(estado -> personas.guardar(persona(email)));

        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado -> personas.guardar(persona(email))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(error -> assertThat(NestedExceptionUtils.getMostSpecificCause(error))
                        .isInstanceOfSatisfying(SQLException.class, sql -> assertThat(sql.getSQLState()).isEqualTo("23505")));
    }

    @Test
    @DisplayName("La reconciliación del contador y el aviso de inactividad (sentencias nativas) corren en PostgreSQL")
    void sentenciasNativas() {
        UUID comedor = UUID.randomUUID();
        Donacion donacion = new Donacion();
        donacion.setIdDonante(UUID.randomUUID());
        donacion.setSubcategoria(new Subcategoria("Arroz"));
        donacion.agregarBien(BienGenerico.builder().subcategoria(new Subcategoria("Arroz"))
                .descripcion("Arroz").cantidad(1).unidad("kg").build());
        donacion.asignarA(EntidadBeneficiaria.builder().id(comedor).razonSocial("Comedor").build());
        transaccion.executeWithoutResult(estado -> donaciones.cargarDonaciones(List.of(donacion)));

        transaccion.executeWithoutResult(estado -> recepciones.reconciliar());
        assertThat(recepciones.contarRecepcionesDesde(comedor, FechaHoraArgentina.hoy().minusDays(1))).isEqualTo(1);

        PersonaHumana inactiva = persona("inactiva" + UUID.randomUUID() + "@mail.com");
        transaccion.executeWithoutResult(estado -> personas.guardar(inactiva));
        LocalDateTime cuando = LocalDateTime.of(2026, 10, 28, 0, 0);
        transaccion.executeWithoutResult(estado -> personas.registrarAvisoDeInactividad(List.of(inactiva.getId()), cuando));
        assertThat(personas.buscarPorId(inactiva.getId()).orElseThrow().getUltimoAvisoInactividad()).isEqualTo(cuando);
    }

    @Test
    @DisplayName("El bloqueo de tareas funciona con la hora de PostgreSQL")
    void bloqueoDeTareas() {
        LockConfiguration configuracion = new LockConfiguration(Instant.now(), "prueba-postgres",
                Duration.ofMinutes(1), Duration.ZERO);
        SimpleLock tomado = bloqueos.lock(configuracion).orElseThrow();

        assertThat(bloqueos.lock(configuracion)).isEmpty();
        tomado.unlock();
        assertThat(bloqueos.lock(configuracion)).isPresent();
    }
}
