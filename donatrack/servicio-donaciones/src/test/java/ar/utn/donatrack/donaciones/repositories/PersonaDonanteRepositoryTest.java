package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.models.contacto.Telefono;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.repositories.jpa.PersonaDonanteJpaRepository;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
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

        PersonaDonante encontrada = repositorio.buscarPorEmail("ANA.PEREZ@mail.com").orElseThrow();

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

        assertThat(repositorio.buscarPorId(persona.getId()).orElseThrow().getEstado().nombre()).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("El listado paginado ordena por email y filtra por estado en la base")
    void listadoPaginadoPorEmail() {
        repositorio.guardar(humana("carla@mail.com"));
        repositorio.guardar(humana("ana@mail.com"));
        PersonaHumana bloqueada = humana("beto@mail.com");
        bloqueada.cambiarEstado("BLOQUEADO", "fraude");
        repositorio.guardar(bloqueada);
        jpa.flush();
        em.clear();

        Pagina<PersonaDonante> primera = repositorio.buscar(null, new SolicitudPagina(0, 2));
        Pagina<PersonaDonante> bloqueadas = repositorio.buscar("BLOQUEADO", new SolicitudPagina(0, 20));

        assertThat(primera.contenido()).extracting(PersonaDonante::getEmail)
                .containsExactly("ana@mail.com", "beto@mail.com");
        assertThat(primera.totalElementos()).isEqualTo(3);
        assertThat(primera.totalPaginas()).isEqualTo(2);
        assertThat(bloqueadas.contenido()).extracting(PersonaDonante::getEmail).containsExactly("beto@mail.com");
    }

    @Test
    @DisplayName("La consulta de inactividad trae a quienes no interactúan desde el límite o nunca interactuaron, salvo bloqueadas o ya avisadas")
    void consultaDeInactivos() {
        LocalDateTime ahora = LocalDateTime.of(2026, 10, 26, 0, 0);
        PersonaHumana reciente = humana("reciente@mail.com");
        reciente.setUltimaInteraccion(ahora.minusDays(5));
        PersonaHumana vieja = humana("vieja@mail.com");
        vieja.setUltimaInteraccion(ahora.minusDays(30));
        PersonaHumana nunca = humana("nunca@mail.com");
        nunca.setUltimaInteraccion(null);
        PersonaHumana bloqueada = humana("bloqueada@mail.com");
        bloqueada.setUltimaInteraccion(ahora.minusDays(30));
        bloqueada.cambiarEstado("BLOQUEADO", "fraude");
        PersonaHumana yaAvisada = humana("avisada@mail.com");
        yaAvisada.setUltimaInteraccion(ahora.minusDays(30));
        yaAvisada.setUltimoAvisoInactividad(ahora.minusDays(3));
        repositorio.guardar(reciente);
        repositorio.guardar(vieja);
        repositorio.guardar(nunca);
        repositorio.guardar(bloqueada);
        repositorio.guardar(yaAvisada);
        jpa.flush();
        em.clear();

        List<PersonaDonante> inactivas = repositorio.obtenerParaAvisoDeInactividad(ahora.minusDays(20), 100);

        assertThat(inactivas).extracting(PersonaDonante::getEmail)
                .containsExactlyInAnyOrder("vieja@mail.com", "nunca@mail.com");

        // Una vez avisadas, dejan de aparecer hasta que pase otro período.
        repositorio.registrarAvisoDeInactividad(inactivas.stream().map(PersonaDonante::getId).toList(), ahora);
        assertThat(repositorio.obtenerParaAvisoDeInactividad(ahora.minusDays(20), 100)).isEmpty();
    }

    @Test
    @DisplayName("Un contacto EMAIL también se guarda en forma canónica")
    void contactoEmailCanonico() {
        PersonaHumana persona = humana("ana@mail.com");
        persona.definirContactoPredeterminado(Email.builder().valor(" Ana@Mail.com").build());

        repositorio.guardar(persona);
        jpa.flush();
        em.clear();

        assertThat(repositorio.buscarPorId(persona.getId()).orElseThrow().getMedioContactoPredeterminado().getValor())
                .isEqualTo("ana@mail.com");
    }

    @Test
    @DisplayName("El contacto predeterminado es uno de los contactos: cambiar el contacto de ese tipo cambia el canal")
    void predeterminadoDentroDeLosContactos() {
        PersonaHumana persona = humana("ana@mail.com");
        persona.definirContactoPredeterminado(Telefono.builder().valor("1111").build());
        repositorio.guardar(persona);
        jpa.flush();
        em.clear();

        PersonaDonante leida = repositorio.buscarPorId(persona.getId()).orElseThrow();
        leida.reemplazarContacto(Telefono.builder().valor("2222").build());
        repositorio.guardar(leida);
        jpa.flush();
        em.clear();

        PersonaDonante releida = repositorio.buscarPorId(persona.getId()).orElseThrow();
        assertThat(releida.getContactos()).hasSize(1);
        assertThat(releida.getMedioContactoPredeterminado().getValor()).isEqualTo("2222");
    }
}
