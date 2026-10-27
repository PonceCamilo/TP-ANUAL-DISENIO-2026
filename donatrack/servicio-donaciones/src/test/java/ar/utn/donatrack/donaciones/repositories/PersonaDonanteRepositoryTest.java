package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.repositories.jpa.PersonaDonanteJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistencia real de personas donantes (Entrega 5): guarda y relee contra H2
 * en modo PostgreSQL, sin mocks.
 *
 * Cubre lo que un test con el repositorio mockeado no puede ver: que la base
 * rechace emails duplicados, que lo que llega a la columna sea la forma
 * canónica y que la búsqueda por email la encuentre.
 */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:persistencia_donantes;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PersonaDonanteRepository.class)
@DisplayName("PersonaDonanteRepository - persistencia real del email y del estado")
class PersonaDonanteRepositoryTest {

    @Autowired private PersonaDonanteRepository repositorio;
    @Autowired private PersonaDonanteJpaRepository jpa;
    @Autowired private TestEntityManager em;

    private PersonaHumana humana(String email) {
        return PersonaHumana.builder()
                .id(UUID.randomUUID())
                .nombre("Ana")
                .apellido("Pérez")
                .tipoDocumento("DNI")
                .numeroDocumento("30111222")
                .email(email)
                .build();
    }

    @Test
    @DisplayName("Guarda el email en forma canónica y lo encuentra aunque se busque con otras mayúsculas")
    void guardaYBuscaEnFormaCanonica() {
        repositorio.guardar(humana("  Ana.Perez@Mail.COM "));
        jpa.flush();
        em.clear();

        PersonaDonante encontrada = repositorio.obtenerPorEmail("ANA.PEREZ@mail.com");

        assertThat(encontrada).isNotNull();
        assertThat(encontrada.getEmail()).isEqualTo("ana.perez@mail.com");
        assertThat(repositorio.existePorEmail(" ana.perez@MAIL.com")).isTrue();
    }

    @Test
    @DisplayName("La base rechaza un segundo donante con el mismo email aunque cambien las mayúsculas")
    void laBaseRechazaDuplicados() {
        // Es la situación de dos altas simultáneas: las dos pasan la validación
        // previa del servicio, y lo único que lo impide es el UNIQUE.
        repositorio.guardar(humana("ana@mail.com"));
        jpa.flush();

        repositorio.guardar(humana("ANA@mail.com"));

        assertThatThrownBy(() -> jpa.flush()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Un donante construido con builder() arranca ACTIVO y se puede guardar")
    void builderArrancaActivo() {
        // Antes de @Builder.Default el estado quedaba en null y fallaba el NOT NULL.
        PersonaHumana persona = humana("nuevo@mail.com");
        assertThat(persona.getEstado()).isNotNull();

        repositorio.guardar(persona);
        jpa.flush();
        em.clear();

        assertThat(repositorio.obtenerPersona(persona.getId()).getEstado().nombre()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("Un contacto EMAIL también se guarda en forma canónica")
    void contactoEmailCanonico() {
        PersonaHumana persona = humana("ana@mail.com");
        persona.setMedioContactoPredeterminado(Email.builder().valor(" Ana@Mail.com").build());

        repositorio.guardar(persona);
        jpa.flush();
        em.clear();

        assertThat(repositorio.obtenerPersona(persona.getId()).getMedioContactoPredeterminado().getValor())
                .isEqualTo("ana@mail.com");
    }
}
