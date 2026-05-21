package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.excepcion.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.model.contacto.TipoMedioContacto;
import ar.utn.donatrack.donaciones.model.donante.*;
import ar.utn.donatrack.donaciones.model.entidad.Direccion;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonaDonanteService - adaptación de tests")
class PersonaDonanteServicioTest {

    @Mock
    private PersonaDonanteRepository repositorio;

    private PersonaDonanteService servicio;

    private static final Direccion direccion = Direccion.builder()
        .calle("Corrientes").numero(1000).localidad("CABA").provincia("Buenos Aires").codigoPostal("1043")
        .build();

    private static final PersonaHumanaDonante donanteHumano = PersonaHumanaDonante.builder()
        .nombre("Ana").apellido("Perez").edad(30)
        .numeroDocumento("12345678").genero(Genero.FEMENINO)
        .direccion(direccion)
        .email("ana@mail.com")
        .medioContactoPredeterminado(TipoMedioContacto.EMAIL)
        .estado(EstadoDonante.ACTIVO)
        .build();

    private static final PersonaHumanaDonante donanteHumano2 = PersonaHumanaDonante.builder()
        .nombre("Luis").apellido("Gomez").edad(45)
        .numeroDocumento("87654321").genero(Genero.MASCULINO)
        .direccion(direccion)
        .email("luis@mail.com")
        .medioContactoPredeterminado(TipoMedioContacto.TELEFONO)
        .estado(EstadoDonante.ACTIVO)
        .build();

    private static final PersonaJuridicaDonante donanteJuridico = PersonaJuridicaDonante.builder()
        .razonSocial("Arcos Plateados S.A.")
        .tipo(TipoPersonaJuridica.EMPRESA)
        .rubro("Tecnologia")
        .email("contacto@empresa.com")
        .medioContactoPredeterminado(TipoMedioContacto.EMAIL)
        .estado(EstadoDonante.ACTIVO)
        .build();

    @BeforeEach
    void setUp() {
        servicio = new PersonaDonanteService(repositorio);
    }

    @Nested
    @DisplayName("registrar() - flujo basico para PersonaHumanaDonante")
    class RegistrarPersonaHumana {

        @Test
        @DisplayName("Flujo completo: valida y guarda")
        void flujoCompleto() {
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);

            servicio.registrar(donanteHumano);

            verify(repositorio).guardar(donanteHumano);
        }

        @Test
        @DisplayName("Verifica que se guarde el donante con los datos correctos")
        void verificaGuardado() {
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);

            servicio.registrar(donanteHumano);

            ArgumentCaptor<PersonaDonante> captor = ArgumentCaptor.forClass(PersonaDonante.class);
            verify(repositorio).guardar(captor.capture());

            PersonaHumanaDonante guardada = (PersonaHumanaDonante) captor.getValue();
            assertEquals("Ana", guardada.getNombre());
            assertEquals(EstadoDonante.ACTIVO, guardada.getEstado());
        }

        @Test
        @DisplayName("El donante guardado tiene el medio de contacto predeterminado correcto")
        void estableceMedioPredeterminado() {
            when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);

            servicio.registrar(donanteHumano);

            ArgumentCaptor<PersonaDonante> captor = ArgumentCaptor.forClass(PersonaDonante.class);
            verify(repositorio).guardar(captor.capture());
            assertEquals(TipoMedioContacto.EMAIL, captor.getValue().getMedioContactoPredeterminado());
        }
    }

    @Nested
    @DisplayName("registrar() para PersonaJuridicaDonante")
    class RegistrarPersonaJuridica {

        @Test
        @DisplayName("Flujo completo: valida y guarda")
        void flujoCompleto() {
            when(repositorio.existePorEmail("contacto@empresa.com")).thenReturn(false);

            servicio.registrar(donanteJuridico);

            verify(repositorio).guardar(donanteJuridico);
        }

        @Test
        @DisplayName("Lanza excepcion si el email ya esta registrado")
        void lanzaExcepcionEmailDuplicado() {
            when(repositorio.existePorEmail("contacto@empresa.com")).thenReturn(true);

            assertThrows(EmailYaRegistradoException.class, () -> servicio.registrar(donanteJuridico));

            verify(repositorio, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("darDeBaja() - delega al repositorio")
    class DarDeBaja {

        @Test
        @DisplayName("Llama a darDeBaja en el repositorio")
        void llamaRepositorioDarDeBaja() {
            UUID id = UUID.randomUUID();
            doNothing().when(repositorio).darDeBaja(id);

            servicio.darDeBaja(id);

            verify(repositorio).darDeBaja(id);
        }

        @Test
        @DisplayName("Lanza NullPointerException si el ID no existe")
        void lanzaExcepcionSiIdNoExiste() {
            PersonaDonanteRepository repoReal = new PersonaDonanteRepository();
            PersonaDonanteService servicioReal = new PersonaDonanteService(repoReal);

            UUID idInexistente = UUID.randomUUID();

            assertThrows(NullPointerException.class, () -> servicioReal.darDeBaja(idInexistente));
        }
    }

    @Test
    @DisplayName("obtenerPorId() delega al repositorio")
    void obtenerPorId() {
        when(repositorio.obtenerPorId(donanteHumano.getId())).thenReturn(donanteHumano);

        PersonaDonante resultado = servicio.obtenerPorId(donanteHumano.getId());

        assertNotNull(resultado);
        assertEquals(donanteHumano.getId(), resultado.getId());
    }

    @Test
    @DisplayName("listarDonantesActivos() delega al repositorio")
    void listarActivos() {
        when(repositorio.obtenerTodosActivos()).thenReturn(List.of(donanteHumano));

        List<PersonaDonante> resultado = servicio.listarDonantesActivos();

        assertEquals(1, resultado.size());
        assertEquals(EstadoDonante.ACTIVO, resultado.getFirst().getEstado());
    }

    @Test
    @DisplayName("Registrar multiples donantes: verifica llamadas al repositorio")
    void registrarMultiplesDonantes() {
        when(repositorio.existePorEmail("ana@mail.com")).thenReturn(false);
        when(repositorio.existePorEmail("luis@mail.com")).thenReturn(false);

        servicio.registrar(donanteHumano);
        servicio.registrar(donanteHumano2);

        verify(repositorio, times(2)).guardar(any());
    }
}