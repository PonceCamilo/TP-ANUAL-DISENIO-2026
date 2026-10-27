package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import ar.utn.donatrack.donaciones.repositories.jpa.EntidadBeneficiariaJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistencia real de entidades beneficiarias, campañas y necesidades
 * (Entrega 5), contra H2 en modo PostgreSQL.
 */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:persistencia_entidades;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EntidadesBeneficiariasRepository.class)
@DisplayName("EntidadesBeneficiariasRepository - persistencia real del agregado de entidades")
class EntidadesBeneficiariasRepositoryTest {

    @Autowired private EntidadesBeneficiariasRepository repositorio;
    @Autowired private EntidadBeneficiariaJpaRepository jpa;
    @Autowired private TestEntityManager em;

    private EntidadBeneficiaria entidadConCampania() {
        UUID idEntidad = UUID.randomUUID();
        NecesidadRecurrente fideos = new NecesidadRecurrente();
        fideos.setNombre("Fideos secos");
        fideos.setCantidadObjetivo(100);
        fideos.setPeriodo(Periodicidad.SEMANAL);
        fideos.setFechaInicioPeriodo(LocalDate.of(2026, 10, 20));
        fideos.setFechaRegistro(LocalDate.of(2026, 10, 20));
        NecesidadExtraordinaria sillas = new NecesidadExtraordinaria();
        sillas.setNombre("Sillas");
        sillas.setCantidadObjetivo(30);
        sillas.setFechaRegistro(LocalDate.of(2026, 10, 21));

        Campania campania = new Campania();
        campania.setIdCampania(UUID.randomUUID());
        campania.setIdEntidad(idEntidad);
        campania.setFechaInicio(LocalDate.of(2026, 10, 1));
        campania.setFechaFin(LocalDate.of(2026, 12, 31));
        campania.agregarNecesidad(fideos);
        campania.agregarNecesidad(sillas);

        return EntidadBeneficiaria.builder()
                .id(idEntidad)
                .razonSocial("Comedor Escobar Sonrisas")
                .contactos(new ArrayList<>())
                .representantes(new ArrayList<>())
                .campanias(new ArrayList<>(List.of(campania)))
                .build();
    }

    @Test
    @DisplayName("idEntidad de la campaña se lee de la FK entidad_id: ya no hay una columna aparte")
    void idEntidadSaleDeLaFk() {
        EntidadBeneficiaria entidad = entidadConCampania();
        repositorio.guardar(entidad);
        jpa.flush();
        em.clear();

        Campania leida = repositorio.obtenerPorId(entidad.getId()).getCampanias().getFirst();

        assertThat(leida.getIdEntidad()).isEqualTo(entidad.getId());
        Number columnasViejas = (Number) em.getEntityManager().createNativeQuery(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE LOWER(TABLE_NAME) = 'campania' AND LOWER(COLUMN_NAME) = 'id_entidad'")
                .getSingleResult();
        assertThat(columnasViejas.longValue()).isZero();
    }

    @Test
    @DisplayName("Las necesidades vuelven con su subtipo y su fecha de registro")
    void necesidadesConSubtipoYFecha() {
        EntidadBeneficiaria entidad = entidadConCampania();
        repositorio.guardar(entidad);
        jpa.flush();
        em.clear();

        Campania leida = repositorio.obtenerPorId(entidad.getId()).getCampanias().getFirst();

        assertThat(leida.getNecesidades()).hasSize(2);
        assertThat(leida.getNecesidades()).anySatisfy(necesidad -> {
            assertThat(necesidad).isInstanceOf(NecesidadRecurrente.class);
            assertThat(((NecesidadRecurrente) necesidad).getPeriodo()).isEqualTo(Periodicidad.SEMANAL);
            assertThat(necesidad.getFechaRegistro()).isEqualTo(LocalDate.of(2026, 10, 20));
        });
        assertThat(leida.getNecesidades()).anySatisfy(necesidad ->
                assertThat(necesidad).isInstanceOf(NecesidadExtraordinaria.class));
    }

    @Test
    @DisplayName("Borrar la entidad borra en cascada sus campañas y necesidades")
    void borradoEnCascada() {
        EntidadBeneficiaria entidad = entidadConCampania();
        repositorio.guardar(entidad);
        jpa.flush();
        em.clear();

        repositorio.eliminar(entidad.getId());
        jpa.flush();

        for (String tabla : List.of("campania", "necesidad", "necesidad_recurrente", "necesidad_extraordinaria")) {
            Number filas = (Number) em.getEntityManager()
                    .createNativeQuery("SELECT COUNT(*) FROM " + tabla).getSingleResult();
            assertThat(filas.longValue()).as(tabla).isZero();
        }
    }
}
