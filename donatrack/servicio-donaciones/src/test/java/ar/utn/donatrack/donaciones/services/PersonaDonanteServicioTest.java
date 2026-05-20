package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.model.*;
import ar.utn.donatrack.donaciones.model.contacto.*;
import ar.utn.donatrack.donaciones.excepcion.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.excepcion.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.notificacion.ObservadorDeRegistro;
import ar.utn.donatrack.donaciones.repositories.PersonaDonanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonaDonanteService - Template Method, Observer, Builder, Factory")
class PersonaDonanteServicioTest {

    @Mock
    private PersonaDonanteRepository repositorio;

    @Mock
    private ObservadorDeRegistro observador;

    private PersonaDonanteServicio servicio;

    private static final Direccion DIRECCION =
            new Direccion("Corrientes", "1000", "CABA", "Buenos Aires", "1043");

    @BeforeEach
    void setUp() {
        servicio = new PersonaDonanteServicio(repositorio, List.of(observador));
    }

    // ─── Registro persona humana ──────────────────────────────────────────────

    @Nested
    @DisplayName("registrarPersonaHumana() - Template Method")
    class RegistrarPersonaHumana {

        @Test
        @DisplayName("Flujo completo: valida, construye, guarda y notifica")
        void flujoCompleto() {
            List<MedioDeContacto> medios = List.of(new Email("ana@mail.com"));
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            PersonaHumana resultado = servicio.registrarPersonaHumana(
                    "Ana", "Pérez", 30, "12345678", Genero.FEMENINO,
                    DIRECCION, medios, medios.get(0));

            assertEquals("Ana", resultado.getNombre());
            assertEquals(EstadoDonante.ACTIVO, resultado.getEstado());

            // Verifica paso 5: se guardó
            verify(repositorio).guardar(any(PersonaHumana.class));

            // Verifica paso 6: se notificó al observer
            verify(observador).alRegistrarDonante(any(PersonaDonante.class));
        }

        @Test
        @DisplayName("Lanza EmailYaRegistradoException si el email ya existe")
        void lanzaExcepcionEmailDuplicado() {
            List<MedioDeContacto> medios = List.of(new Email("ana@mail.com"));
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(true);

            assertThrows(EmailYaRegistradoException.class, () ->
                    servicio.registrarPersonaHumana(
                            "Ana", "Pérez", 30, "12345678", Genero.FEMENINO,
                            DIRECCION, medios, null));

            // Nunca debe guardar ni notificar si la validación falla
            verify(repositorio, never()).guardar(any());
            verify(observador, never()).alRegistrarDonante(any());
        }

        @Test
        @DisplayName("El Observer recibe la persona correcta")
        void observerRecibeLaPersonaCorrecta() {
            List<MedioDeContacto> medios = List.of(new Email("ana@mail.com"));
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            servicio.registrarPersonaHumana(
                    "Ana", "Pérez", 30, "12345678", Genero.FEMENINO,
                    DIRECCION, medios, null);

            ArgumentCaptor<PersonaDonante> captor = ArgumentCaptor.forClass(PersonaDonante.class);
            verify(observador).alRegistrarDonante(captor.capture());

            PersonaHumana notificada = (PersonaHumana) captor.getValue();
            assertEquals("Ana", notificada.getNombre());
            assertEquals(EstadoDonante.ACTIVO, notificada.getEstado());
        }

        @Test
        @DisplayName("Establece el medio predeterminado correctamente")
        void estableceMedioPredeterminado() {
            Email email = new Email("ana@mail.com");
            WhatsApp wp = new WhatsApp("+5411999");
            List<MedioDeContacto> medios = List.of(email, wp);
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            PersonaHumana resultado = servicio.registrarPersonaHumana(
                    "Ana", "Pérez", 30, "12345678", Genero.FEMENINO,
                    DIRECCION, medios, wp); // WhatsApp como predeterminado

            assertEquals("+5411999", resultado.getMedioContactoPredeterminado().getValor());
        }
    }

    // ─── Registro persona jurídica ────────────────────────────────────────────

    @Nested
    @DisplayName("registrarPersonaJuridica() - Template Method")
    class RegistrarPersonaJuridica {

        @Test
        @DisplayName("Flujo completo: valida, construye, guarda y notifica")
        void flujoCompleto() {
            List<MedioDeContacto> medios = List.of(new Email("contacto@empresa.com"));
            List<Representante> reps = List.of(
                    new Representante("Carlos", "Ruiz", "rep@empresa.com", null));
            when(repositorio.existePorEmail("contacto@empresa.com")).thenReturn(false);
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            PersonaJuridica resultado = servicio.registrarPersonaJuridica(
                    "Arcos Plateados S.A.", TipoPersonaJuridica.EMPRESA,
                    "Tecnología", medios, medios.get(0), reps);

            assertEquals("Arcos Plateados S.A.", resultado.getRazonSocial());
            assertEquals(EstadoDonante.ACTIVO, resultado.getEstado());

            verify(repositorio).guardar(any(PersonaJuridica.class));
            verify(observador).alRegistrarDonante(any(PersonaDonante.class));
        }

        @Test
        @DisplayName("Lanza excepción si el email ya está registrado")
        void lanzaExcepcionEmailDuplicado() {
            List<MedioDeContacto> medios = List.of(new Email("contacto@empresa.com"));
            when(repositorio.existePorEmail("contacto@empresa.com")).thenReturn(true);

            assertThrows(EmailYaRegistradoException.class, () ->
                    servicio.registrarPersonaJuridica(
                            "Arcos S.A.", TipoPersonaJuridica.EMPRESA, "Tech",
                            medios, null, List.of()));

            verify(repositorio, never()).guardar(any());
        }
    }

    // ─── darDeBaja ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("darDeBaja() - State")
    class DarDeBaja {

        @Test
        @DisplayName("Cambia el estado de la persona a INACTIVO")
        void cambiaEstadoAInactivo() {
            PersonaHumana persona = humanaEnMemoria("ana@mail.com");
            when(repositorio.buscarPorId(persona.getId())).thenReturn(Optional.of(persona));
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            servicio.darDeBaja(persona.getId());

            assertEquals(EstadoDonante.INACTIVO, persona.getEstado());
            verify(repositorio).guardar(persona);
        }

        @Test
        @DisplayName("Lanza PersonaDonanteNoEncontradaException si el ID no existe")
        void lanzaExcepcionSiIdNoExiste() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThrows(PersonaDonanteNoEncontradaException.class, () ->
                    servicio.darDeBaja(idInexistente));
        }
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId() delega al repositorio")
    void buscarPorId() {
        PersonaHumana persona = humanaEnMemoria("ana@mail.com");
        when(repositorio.buscarPorId(persona.getId())).thenReturn(Optional.of(persona));

        Optional<PersonaDonante> resultado = servicio.buscarPorId(persona.getId());

        assertTrue(resultado.isPresent());
        assertEquals(persona.getId(), resultado.get().getId());
    }

    @Test
    @DisplayName("listarActivos() delega al repositorio")
    void listarActivos() {
        PersonaHumana persona = humanaEnMemoria("ana@mail.com");
        when(repositorio.buscarActivos()).thenReturn(List.of(persona));

        List<PersonaDonante> resultado = servicio.listarActivos();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).isActivo());
    }

    // ─── Observers múltiples ──────────────────────────────────────────────────

    @Test
    @DisplayName("Notifica a TODOS los observers registrados")
    void notificaTodosLosObservers() {
        ObservadorDeRegistro obs2 = mock(ObservadorDeRegistro.class);
        PersonaDonanteServicio servicioConDosObs =
                new PersonaDonanteServicio(repositorio, List.of(observador, obs2));

        List<MedioDeContacto> medios = List.of(new Email("ana@mail.com"));
        when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
        when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        servicioConDosObs.registrarPersonaHumana(
                "Ana", "Pérez", 30, "12345678", Genero.FEMENINO,
                DIRECCION, medios, null);

        verify(observador, times(1)).alRegistrarDonante(any());
        verify(obs2, times(1)).alRegistrarDonante(any());
    }

    // ─── Factory Method expuesto ──────────────────────────────────────────────

    @Test
    @DisplayName("crearMedioDeContacto() crea la instancia correcta según el tipo")
    void crearMedioDeContacto() {
        MedioDeContacto email = PersonaDonanteServicio.crearMedioDeContacto(
                TipoMedioContacto.EMAIL, "a@b.com");
        MedioDeContacto tel = PersonaDonanteServicio.crearMedioDeContacto(
                TipoMedioContacto.TELEFONO, "+5411");

        assertInstanceOf(Email.class, email);
        assertInstanceOf(Telefono.class, tel);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private PersonaHumana humanaEnMemoria(String emailStr) {
        return new PersonaHumanaBuilder()
                .nombre("Ana").apellido("Pérez").edad(30)
                .numeroDocumento("12345678").genero(Genero.FEMENINO)
                .direccion(DIRECCION)
                .agregarMedio(new Email(emailStr))
                .build();
    }
}
