package com.donatrack.donaciones.repositorio;

import com.donatrack.donaciones.dominio.*;
import com.donatrack.donaciones.dominio.contacto.Email;
import com.donatrack.donaciones.dominio.contacto.Telefono;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PersonaDonanteRepositorioEnMemoria - Patrón SINGLETON")
class PersonaDonanteRepositorioEnMemoriaTest {

    private PersonaDonanteRepositorioEnMemoria repositorio;

    private static final Direccion DIRECCION =
            new Direccion("Corrientes", "1000", "CABA", "Buenos Aires", "1043");

    @BeforeEach
    void setUp() {
        // Cada test arranca con un repositorio limpio
        repositorio = new PersonaDonanteRepositorioEnMemoria();
    }

    // ─── Guardar y buscar por ID ──────────────────────────────────────────────

    @Nested
    @DisplayName("guardar() y buscarPorId()")
    class GuardarYBuscar {

        @Test
        @DisplayName("Guarda una persona y la recupera por ID")
        void guardaYRecuperaPorId() {
            PersonaHumana p = humanaValida("ana@mail.com");
            repositorio.guardar(p);

            Optional<PersonaDonante> resultado = repositorio.buscarPorId(p.getId());
            assertTrue(resultado.isPresent());
            assertEquals(p.getId(), resultado.get().getId());
        }

        @Test
        @DisplayName("Devuelve Optional vacío si el ID no existe")
        void devuelveVacioSiIdNoExiste() {
            Optional<PersonaDonante> resultado = repositorio.buscarPorId(UUID.randomUUID());
            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("guardar() con el mismo ID sobreescribe (upsert)")
        void guardadoConMismoIdSobreescribe() {
            PersonaHumana p = humanaValida("ana@mail.com");
            repositorio.guardar(p);
            p.actualizarDatos("Beatriz", "Lopez", 35, Genero.FEMENINO, DIRECCION);
            repositorio.guardar(p);

            PersonaDonante guardado = repositorio.buscarPorId(p.getId()).orElseThrow();
            assertEquals("Beatriz", ((PersonaHumana) guardado).getNombre());
        }
    }

    // ─── buscarPorEmail ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarPorEmail()")
    class BuscarPorEmail {

        @Test
        @DisplayName("Encuentra una persona por su email exacto")
        void encuentraPorEmail() {
            repositorio.guardar(humanaValida("ana@mail.com"));
            assertTrue(repositorio.buscarPorEmail("ana@mail.com").isPresent());
        }

        @Test
        @DisplayName("Búsqueda por email es case-insensitive")
        void busquedaCaseInsensitive() {
            repositorio.guardar(humanaValida("Ana@Mail.COM"));
            assertTrue(repositorio.buscarPorEmail("ana@mail.com").isPresent());
        }

        @Test
        @DisplayName("Devuelve vacío si el email no existe")
        void devuelveVacioSiEmailNoExiste() {
            assertTrue(repositorio.buscarPorEmail("noexiste@mail.com").isEmpty());
        }
    }

    // ─── existePorEmail ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("existePorEmail()")
    class ExistePorEmail {

        @Test
        @DisplayName("Devuelve true si el email ya está registrado")
        void devuelveTrueSiExiste() {
            repositorio.guardar(humanaValida("ana@mail.com"));
            assertTrue(repositorio.existePorEmail("ana@mail.com"));
        }

        @Test
        @DisplayName("Devuelve false si el email no está registrado")
        void devuelveFalseSiNoExiste() {
            assertFalse(repositorio.existePorEmail("nuevo@mail.com"));
        }
    }

    // ─── buscarActivos ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarActivos()")
    class BuscarActivos {

        @Test
        @DisplayName("Lista solo los donantes con estado ACTIVO")
        void listaSoloActivos() {
            PersonaHumana activo = humanaValida("activo@mail.com");
            PersonaHumana inactivo = humanaValida("inactivo@mail.com");
            inactivo.darDeBaja();

            repositorio.guardar(activo);
            repositorio.guardar(inactivo);

            List<PersonaDonante> activos = repositorio.buscarActivos();
            assertEquals(1, activos.size());
            assertTrue(activos.get(0).isActivo());
        }

        @Test
        @DisplayName("Devuelve lista vacía si no hay activos")
        void devuelveVacioSiNoHayActivos() {
            PersonaHumana p = humanaValida("p@mail.com");
            p.darDeBaja();
            repositorio.guardar(p);

            assertTrue(repositorio.buscarActivos().isEmpty());
        }
    }

    // ─── buscarTodos ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarTodos()")
    class BuscarTodos {

        @Test
        @DisplayName("Lista activos e inactivos")
        void listaActivosEInactivos() {
            PersonaHumana p1 = humanaValida("p1@mail.com");
            PersonaHumana p2 = humanaValida("p2@mail.com");
            p2.darDeBaja();

            repositorio.guardar(p1);
            repositorio.guardar(p2);

            assertEquals(2, repositorio.buscarTodos().size());
        }

        @Test
        @DisplayName("Devuelve lista vacía si el repositorio está vacío")
        void devuelveVacioSiEstaVacio() {
            assertTrue(repositorio.buscarTodos().isEmpty());
        }
    }

    // ─── eliminar ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar() remueve la persona del repositorio")
    void eliminarRemueve() {
        PersonaHumana p = humanaValida("ana@mail.com");
        repositorio.guardar(p);

        repositorio.eliminar(p.getId());

        assertTrue(repositorio.buscarPorId(p.getId()).isEmpty());
        assertFalse(repositorio.existePorEmail("ana@mail.com"));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private PersonaHumana humanaValida(String email) {
        return new PersonaHumanaBuilder()
                .nombre("Ana").apellido("Pérez").edad(30)
                .numeroDocumento("12345678").genero(Genero.FEMENINO)
                .direccion(DIRECCION)
                .agregarMedio(new Email(email))
                .build();
    }
}
