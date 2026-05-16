package com.donatrack.donaciones.servicio;

import com.donatrack.donaciones.dominio.*;
import com.donatrack.donaciones.dominio.contacto.TipoMedioContacto;
import com.donatrack.donaciones.dto.request.*;
import com.donatrack.donaciones.dto.response.PersonaDonanteResponse;
import com.donatrack.donaciones.excepcion.EmailYaRegistradoException;
import com.donatrack.donaciones.excepcion.PersonaDonanteNoEncontradaException;
import com.donatrack.donaciones.notificacion.ObservadorDeRegistro;
import com.donatrack.donaciones.repositorio.PersonaDonanteRepositorio;
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
@DisplayName("PersonaDonanteServicio - Template Method, Observer, Builder, Factory")
class PersonaDonanteServicioTest {

    @Mock
    private PersonaDonanteRepositorio repositorio;

    @Mock
    private ObservadorDeRegistro observador;

    private PersonaDonanteServicio servicio;

    @BeforeEach
    void setUp() {
        // Inyectamos el mock del observer en la lista — así probamos el patrón OBSERVER
        servicio = new PersonaDonanteServicio(repositorio, List.of(observador));
    }

    // ─── Registro persona humana ──────────────────────────────────────────────

    @Nested
    @DisplayName("registrarPersonaHumana() - Template Method")
    class RegistrarPersonaHumana {

        @Test
        @DisplayName("Flujo completo: valida, construye, guarda y notifica")
        void flujoCompleto() {
            // Arrange
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            RegistroPersonaHumanaRequest request = requestHumanaValida("ana@mail.com");

            // Act
            PersonaDonanteResponse response = servicio.registrarPersonaHumana(request);

            // Assert - resultado correcto
            assertEquals("HUMANA", response.tipoPersona());
            assertEquals("Ana", response.nombre());
            assertEquals("Pérez", response.apellido());
            assertEquals("ACTIVO", response.estado());   // ← verifica patrón STATE en la respuesta

            // Assert - se guardó en el repositorio (paso 5 del template)
            verify(repositorio, times(1)).guardar(any(PersonaHumana.class));

            // Assert - se notificó al observer (paso 6 del template)
            verify(observador, times(1)).alRegistrarDonante(any(PersonaDonante.class));
        }

        @Test
        @DisplayName("Lanza EmailYaRegistradoException si el email ya existe")
        void lanzaExcepcionSiEmailDuplicado() {
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(true);

            assertThrows(EmailYaRegistradoException.class, () ->
                    servicio.registrarPersonaHumana(requestHumanaValida("ana@mail.com"))
            );

            // Verifica que nunca se llegó a guardar ni notificar
            verify(repositorio, never()).guardar(any());
            verify(observador, never()).alRegistrarDonante(any());
        }

        @Test
        @DisplayName("El Observer recibe la persona correcta al notificar")
        void observerRecibeLaPersonaCorrecta() {
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            servicio.registrarPersonaHumana(requestHumanaValida("ana@mail.com"));

            // Captura el argumento que recibió el observer
            ArgumentCaptor<PersonaDonante> captor = ArgumentCaptor.forClass(PersonaDonante.class);
            verify(observador).alRegistrarDonante(captor.capture());

            PersonaHumana notificada = (PersonaHumana) captor.getValue();
            assertEquals("Ana", notificada.getNombre());
            assertEquals(EstadoDonante.ACTIVO, notificada.getEstado());
        }

        @Test
        @DisplayName("Configura el medio predeterminado marcado en el request")
        void configuraMedioPredeterminadoMarcado() {
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            // Request con WhatsApp marcado como predeterminado
            RegistroPersonaHumanaRequest request = new RegistroPersonaHumanaRequest(
                    "Ana", "Pérez", 30, "12345678", Genero.FEMENINO,
                    direccionRequest(),
                    List.of(
                            new MedioDeContactoRequest(TipoMedioContacto.EMAIL, "ana@mail.com", false),
                            new MedioDeContactoRequest(TipoMedioContacto.WHATSAPP, "+5411999", true)
                    )
            );

            PersonaDonanteResponse response = servicio.registrarPersonaHumana(request);

            // El predeterminado debe ser el WhatsApp
            assertEquals("+5411999", response.medioContactoPredeterminado());
        }
    }

    // ─── Registro persona jurídica ────────────────────────────────────────────

    @Nested
    @DisplayName("registrarPersonaJuridica() - Template Method")
    class RegistrarPersonaJuridica {

        @Test
        @DisplayName("Flujo completo: valida, construye, guarda y notifica")
        void flujoCompleto() {
            when(repositorio.existePorEmail("contacto@empresa.com")).thenReturn(false);
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            PersonaDonanteResponse response =
                    servicio.registrarPersonaJuridica(requestJuridicaValida("contacto@empresa.com"));

            assertEquals("JURIDICA", response.tipoPersona());
            assertEquals("Arcos Plateados S.A.", response.razonSocial());
            assertEquals("ACTIVO", response.estado());

            verify(repositorio).guardar(any(PersonaJuridica.class));
            verify(observador).alRegistrarDonante(any(PersonaDonante.class));
        }

        @Test
        @DisplayName("Lanza excepción si el email ya está registrado")
        void lanzaExcepcionEmailDuplicado() {
            when(repositorio.existePorEmail("contacto@empresa.com")).thenReturn(true);

            assertThrows(EmailYaRegistradoException.class, () ->
                    servicio.registrarPersonaJuridica(requestJuridicaValida("contacto@empresa.com"))
            );

            verify(repositorio, never()).guardar(any());
        }
    }

    // ─── obtenerPorId ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("Devuelve la persona si existe")
        void devuelvePersonaSiExiste() {
            PersonaHumana p = humanaEnMemoria("ana@mail.com");
            when(repositorio.buscarPorId(p.getId())).thenReturn(Optional.of(p));

            PersonaDonanteResponse response = servicio.obtenerPorId(p.getId());

            assertEquals(p.getId(), response.id());
        }

        @Test
        @DisplayName("Lanza PersonaDonanteNoEncontradaException si no existe")
        void lanzaExcepcionSiNoExiste() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThrows(PersonaDonanteNoEncontradaException.class, () ->
                    servicio.obtenerPorId(idInexistente)
            );
        }
    }

    // ─── darDeBaja ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("darDeBaja() - State")
    class DarDeBaja {

        @Test
        @DisplayName("Cambia el estado de la persona a INACTIVO")
        void cambiaEstadoAInactivo() {
            PersonaHumana p = humanaEnMemoria("ana@mail.com");
            when(repositorio.buscarPorId(p.getId())).thenReturn(Optional.of(p));
            when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            servicio.darDeBaja(p.getId());

            assertEquals(EstadoDonante.INACTIVO, p.getEstado());
            verify(repositorio).guardar(p);
        }

        @Test
        @DisplayName("Lanza excepción si el ID no existe")
        void lanzaExcepcionSiIdNoExiste() {
            UUID id = UUID.randomUUID();
            when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

            assertThrows(PersonaDonanteNoEncontradaException.class, () ->
                    servicio.darDeBaja(id)
            );
        }
    }

    // ─── listarActivos ────────────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivos() delega al repositorio y mapea correctamente")
    void listarActivos() {
        PersonaHumana p = humanaEnMemoria("ana@mail.com");
        when(repositorio.buscarActivos()).thenReturn(List.of(p));

        List<PersonaDonanteResponse> resultado = servicio.listarActivos();

        assertEquals(1, resultado.size());
        assertEquals("ACTIVO", resultado.get(0).estado());
    }

    // ─── Observers múltiples ──────────────────────────────────────────────────

    @Test
    @DisplayName("Notifica a TODOS los observers registrados")
    void notificaTodosLosObservers() {
        ObservadorDeRegistro obs2 = mock(ObservadorDeRegistro.class);
        // Servicio con dos observers
        PersonaDonanteServicio servicioConDosObs =
                new PersonaDonanteServicio(repositorio, List.of(observador, obs2));

        when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
        when(repositorio.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        servicioConDosObs.registrarPersonaHumana(requestHumanaValida("ana@mail.com"));

        verify(observador, times(1)).alRegistrarDonante(any());
        verify(obs2, times(1)).alRegistrarDonante(any());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private RegistroPersonaHumanaRequest requestHumanaValida(String email) {
        return new RegistroPersonaHumanaRequest(
                "Ana", "Pérez", 30, "12345678", Genero.FEMENINO,
                direccionRequest(),
                List.of(new MedioDeContactoRequest(TipoMedioContacto.EMAIL, email, true))
        );
    }

    private RegistroPersonaJuridicaRequest requestJuridicaValida(String email) {
        return new RegistroPersonaJuridicaRequest(
                "Arcos Plateados S.A.",
                TipoPersonaJuridica.EMPRESA,
                "Tecnología",
                List.of(new MedioDeContactoRequest(TipoMedioContacto.EMAIL, email, true)),
                List.of(new RepresentanteRequest("Carlos", "Ruiz", "rep@arcos.com", "+5411000"))
        );
    }

    private DireccionRequest direccionRequest() {
        return new DireccionRequest("Corrientes", "1000", "CABA", "Buenos Aires", "1043");
    }

    private PersonaHumana humanaEnMemoria(String email) {
        return new PersonaHumanaBuilder()
                .nombre("Ana").apellido("Pérez").edad(30)
                .numeroDocumento("12345678").genero(Genero.FEMENINO)
                .direccion(new Direccion("Corrientes", "1000", "CABA", "Buenos Aires", "1043"))
                .agregarMedio(new com.donatrack.donaciones.dominio.contacto.Email(email))
                .build();
    }
}
