package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.Representante;
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
        sillas.setSubcategoria(new Subcategoria("Sillas"));
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

        Campania leida = repositorio.buscarPorId(entidad.getId()).orElseThrow().getCampanias().getFirst();

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

        Campania leida = repositorio.buscarPorId(entidad.getId()).orElseThrow().getCampanias().getFirst();

        assertThat(leida.getNecesidades()).hasSize(2);
        assertThat(leida.getNecesidades()).anySatisfy(necesidad -> {
            assertThat(necesidad).isInstanceOf(NecesidadRecurrente.class);
            assertThat(((NecesidadRecurrente) necesidad).getPeriodo()).isEqualTo(Periodicidad.SEMANAL);
            assertThat(necesidad.getFechaRegistro()).isEqualTo(LocalDate.of(2026, 10, 20));
        });
        assertThat(leida.getNecesidades()).anySatisfy(necesidad -> {
            assertThat(necesidad).isInstanceOf(NecesidadExtraordinaria.class);
            assertThat(necesidad.getSubcategoria()).isEqualTo(new Subcategoria("sillas"));
        });
        // La subcategoría es opcional: la de fideos no tiene y vuelve nula.
        assertThat(leida.getNecesidades()).anySatisfy(necesidad -> {
            assertThat(necesidad.getNombre()).isEqualTo("Fideos secos");
            assertThat(necesidad.getSubcategoria()).isNull();
        });
    }

    @Test
    @DisplayName("Borrar la entidad borra en cascada sus campañas y necesidades")
    void borradoEnCascada() {
        EntidadBeneficiaria entidad = entidadConCampania();
        repositorio.guardar(entidad);
        jpa.flush();
        em.clear();

        repositorio.eliminar(entidad);
        jpa.flush();

        for (String tabla : List.of("campania", "necesidad", "necesidad_recurrente", "necesidad_extraordinaria")) {
            Number filas = (Number) em.getEntityManager()
                    .createNativeQuery("SELECT COUNT(*) FROM " + tabla).getSingleResult();
            assertThat(filas.longValue()).as(tabla).isZero();
        }
    }

    @Test
    @DisplayName("El listado es paginado y ordenado por razón social")
    void listadoPaginado() {
        for (String razonSocial : List.of("Escuela 12", "Comedor Los Pibes", "Hogar San José")) {
            repositorio.guardar(EntidadBeneficiaria.builder().razonSocial(razonSocial).build());
        }
        jpa.flush();
        em.clear();

        Pagina<EntidadBeneficiaria> primera = repositorio.buscar(new SolicitudPagina(0, 2));
        Pagina<EntidadBeneficiaria> segunda = repositorio.buscar(new SolicitudPagina(1, 2));

        assertThat(primera.contenido()).extracting(EntidadBeneficiaria::getRazonSocial)
                .containsExactly("Comedor Los Pibes", "Escuela 12");
        assertThat(segunda.contenido()).extracting(EntidadBeneficiaria::getRazonSocial).containsExactly("Hogar San José");
        assertThat(primera.totalElementos()).isEqualTo(3);
        assertThat(primera.totalPaginas()).isEqualTo(2);
    }

    // ── Búsqueda por email (Entrega 6) ────────────────────────────────────────

    private EntidadBeneficiaria entidad(String razonSocial, String emailContacto, String emailRepresentante) {
        List<MedioDeContacto> contactos = new ArrayList<>();
        if (emailContacto != null) {
            contactos.add(Email.builder().valor(emailContacto).build());
        }
        List<Representante> representantes = new ArrayList<>();
        if (emailRepresentante != null) {
            representantes.add(Representante.builder()
                    .id(UUID.randomUUID()).nombre("Ana").apellido("Gómez").email(emailRepresentante)
                    .contactos(new ArrayList<>()).build());
        }
        return EntidadBeneficiaria.builder()
                .razonSocial(razonSocial).contactos(contactos).representantes(representantes).build();
    }

    private void guardarYLimpiar(EntidadBeneficiaria... entidades) {
        for (EntidadBeneficiaria entidad : entidades) {
            repositorio.guardar(entidad);
        }
        jpa.flush();
        em.clear();
    }

    @Test
    @DisplayName("Encuentra a la entidad por el email de un contacto y por el de un representante")
    void buscaPorEmailDeContactoODeRepresentante() {
        // El front ubica así a la entidad de quien inició sesión: esa persona
        // puede ser el contacto de la entidad o uno de sus representantes.
        guardarYLimpiar(
                entidad("Comedor Los Pibes", "comedor@example.com", null),
                entidad("Escuela 12", null, "directora@example.com"),
                entidad("Hogar San José", "hogar@example.com", "otro@example.com"));

        SolicitudPagina pagina = new SolicitudPagina(0, 20);

        assertThat(repositorio.buscarPorEmail("comedor@example.com", pagina).contenido())
                .extracting(EntidadBeneficiaria::getRazonSocial).containsExactly("Comedor Los Pibes");
        assertThat(repositorio.buscarPorEmail("directora@example.com", pagina).contenido())
                .extracting(EntidadBeneficiaria::getRazonSocial).containsExactly("Escuela 12");
    }

    @Test
    @DisplayName("No distingue mayúsculas ni espacios, de ninguno de los dos lados")
    void buscaPorEmailSinDistinguirMayusculas() {
        // Los contactos se guardan como los escribió la persona, así que la
        // comparación normaliza los dos lados.
        guardarYLimpiar(entidad("Comedor Los Pibes", "Comedor@Example.COM", null));

        Pagina<EntidadBeneficiaria> encontrada =
                repositorio.buscarPorEmail("  COMEDOR@example.com ", new SolicitudPagina(0, 20));

        assertThat(encontrada.contenido()).hasSize(1);
        assertThat(encontrada.totalElementos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una entidad con el mismo email como contacto y como representante aparece una sola vez")
    void buscaPorEmailSinDuplicar() {
        guardarYLimpiar(entidad("Comedor Los Pibes", "ana@example.com", "ana@example.com"));

        Pagina<EntidadBeneficiaria> encontrada =
                repositorio.buscarPorEmail("ana@example.com", new SolicitudPagina(0, 20));

        assertThat(encontrada.contenido()).hasSize(1);
        assertThat(encontrada.totalElementos()).isEqualTo(1);
    }

    @Test
    @DisplayName("Si una persona representa a varias entidades, las devuelve todas ordenadas")
    void buscaPorEmailVariasEntidades() {
        guardarYLimpiar(
                entidad("Hogar San José", null, "ana@example.com"),
                entidad("Comedor Los Pibes", null, "ana@example.com"),
                entidad("Escuela 12", null, "otra@example.com"));

        Pagina<EntidadBeneficiaria> encontradas =
                repositorio.buscarPorEmail("ana@example.com", new SolicitudPagina(0, 20));

        assertThat(encontradas.contenido()).extracting(EntidadBeneficiaria::getRazonSocial)
                .containsExactly("Comedor Los Pibes", "Hogar San José");
        assertThat(encontradas.totalElementos()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un email que no tiene ninguna entidad devuelve una página vacía")
    void buscaPorEmailInexistente() {
        guardarYLimpiar(entidad("Comedor Los Pibes", "comedor@example.com", null));

        Pagina<EntidadBeneficiaria> encontrada =
                repositorio.buscarPorEmail("nadie@example.com", new SolicitudPagina(0, 20));

        assertThat(encontrada.contenido()).isEmpty();
        assertThat(encontrada.totalElementos()).isZero();
    }
}
