package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.DireccionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EmailRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.EstadoDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.LocalidadRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaDonanteUpdateRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaHumanaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.PersonaJuridicaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.ProvinciaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.RepresentanteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.TelefonoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaDonanteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaHumanaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.PersonaJuridicaResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.MedioContactoInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.CambioEstadoPersonaIlegalException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.FaltaJustificacionException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaConMismoEstadoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.mappers.PersonaDonanteMapper;
import ar.utn.donatrack.donaciones.models.donante.Genero;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.TipoPersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.estado.ActivoState;
import ar.utn.donatrack.donaciones.models.donante.estado.BloqueadoState;
import ar.utn.donatrack.donaciones.models.donante.estado.InactivoState;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del CRUD de personas donantes.
 *
 * Cubre el alta (con validación de email único), la consulta con filtro por
 * estado, el cambio de estado del donante, la actualización de sus datos según
 * el subtipo (humana / jurídica), la gestión de contactos y representantes,
 * y la baja.
 *
 * Regla transversal que se verifica en varios tests: toda operación del donante
 * REGISTRA UNA INTERACCIÓN. De eso depende la tarea diaria que detecta donantes
 * inactivos: si una operación no sellara la fecha, el donante recibiría avisos
 * de reactivación aunque esté usando el sistema.
 *
 * Se usan el validador y el mapper reales; solo se mockea el repositorio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonaDonanteService - CRUD de personas donantes")
class PersonaDonanteServiceTest {

    @Mock
    private PersonaDonanteRepositoryInterface repositorio;

    @Captor
    private ArgumentCaptor<PersonaDonante> donanteCaptor;

    private PersonaDonanteService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new PersonaDonanteService(
                repositorio,
                new PersonasValidator(repositorio),
                new PersonaDonanteMapper());
    }

    // ── Constructores de DTOs de prueba ──────────────────────────────────────

    private DireccionRequestDTO direccion() {
        return DireccionRequestDTO.builder()
                .calle("Medrano")
                .numero(951)
                .codigoPostal("C1179")
                .localidad(LocalidadRequestDTO.builder()
                        .nombre("CABA")
                        .provincia(ProvinciaRequestDTO.builder().nombre("Buenos Aires").build())
                        .build())
                .build();
    }

    private PersonaHumanaRequestDTO dtoHumana(String email) {
        return PersonaHumanaRequestDTO.builder()
                .nombre("Juan")
                .apellido("Pérez")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .genero(Genero.MASCULINO)
                .tipoDocumento("DNI")
                .numeroDocumento("30111222")
                .email(email)
                .direccion(direccion())
                .medioContactoPredeterminado(EmailRequestDTO.builder().valor(email).build())
                .build();
    }

    private PersonaJuridicaRequestDTO dtoJuridica(String email) {
        return PersonaJuridicaRequestDTO.builder()
                .razonSocial("Fundación Ejemplo")
                .rubro("Alimentos")
                .tipoOrganizacion(TipoPersonaJuridica.ONG)
                .tipoDocumento("CUIT")
                .numeroDocumento("30-11122233-4")
                .email(email)
                .direccion(direccion())
                .medioContactoPredeterminado(EmailRequestDTO.builder().valor(email).build())
                .representantes(List.of(RepresentanteRequestDTO.builder()
                        .nombre("Ana").apellido("Gómez").email("ana@fundacion.org").build()))
                .build();
    }

    // ── Constructores de modelos de prueba ───────────────────────────────────

    private PersonaHumana humanaGuardada(UUID id) {
        return PersonaHumana.builder()
                .id(id)
                .nombre("Juan")
                .apellido("Pérez")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .tipoDocumento("DNI")
                .numeroDocumento("30111222")
                .email("juan@example.com")
                .estado(new ActivoState())
                .build();
    }

    private PersonaJuridica juridicaGuardada(UUID id) {
        return PersonaJuridica.builder()
                .id(id)
                .razonSocial("Fundación Ejemplo")
                .rubro("Alimentos")
                .tipo(TipoPersonaJuridica.ONG)
                .email("contacto@fundacion.org")
                .estado(new ActivoState())
                .build();
    }

    private EstadoDonanteRequestDTO dtoEstado(String estado, String justificacion) {
        EstadoDonanteRequestDTO dto = new EstadoDonanteRequestDTO();
        dto.setEstado(estado);
        dto.setJustificacion(justificacion);
        return dto;
    }

    @Nested
    @DisplayName("Alta de donantes")
    class Alta {

        @Test
        @DisplayName("Registra una persona humana con id generado, estado ACTIVO e interacción inicial")
        void registraPersonaHumana() {
            when(repositorio.existePorEmail("juan@example.com")).thenReturn(false);

            PersonaDonanteResponseDTO respuesta = servicio.registrar(dtoHumana("juan@example.com"));

            assertThat(respuesta).isInstanceOf(PersonaHumanaResponseDTO.class);
            assertThat(respuesta.getId()).isNotNull();
            assertThat(respuesta.getEstado()).isEqualTo("ACTIVO");

            verify(repositorio).guardar(donanteCaptor.capture());
            assertThat(donanteCaptor.getValue().getUltimaInteraccion()).isNotNull();
        }

        @Test
        @DisplayName("Registra una persona jurídica con su razón social y representantes")
        void registraPersonaJuridica() {
            when(repositorio.existePorEmail("contacto@fundacion.org")).thenReturn(false);

            PersonaDonanteResponseDTO respuesta = servicio.registrar(dtoJuridica("contacto@fundacion.org"));

            assertThat(respuesta).isInstanceOf(PersonaJuridicaResponseDTO.class);
            PersonaJuridicaResponseDTO juridica = (PersonaJuridicaResponseDTO) respuesta;
            assertThat(juridica.getRazonSocial()).isEqualTo("Fundación Ejemplo");
            assertThat(juridica.getRepresentantes()).hasSize(1);
        }

        @Test
        @DisplayName("La respuesta de una persona humana calcula la edad a partir de la fecha de nacimiento")
        void calculaLaEdad() {
            // El enunciado pide la edad, no la fecha: el mapper la deriva para
            // que no quede desactualizada con el paso del tiempo.
            when(repositorio.existePorEmail("juan@example.com")).thenReturn(false);

            PersonaHumanaResponseDTO respuesta =
                    (PersonaHumanaResponseDTO) servicio.registrar(dtoHumana("juan@example.com"));

            int edadEsperada = java.time.Period.between(
                    LocalDate.of(1990, 5, 20), LocalDate.now()).getYears();
            assertThat(respuesta.getEdad()).isEqualTo(edadEsperada);
        }

        @Test
        @DisplayName("Rechaza un email con formato inválido y no guarda nada")
        void rechazaEmailInvalido() {
            assertThatThrownBy(() -> servicio.registrar(dtoHumana("sin-arroba")))
                    .isInstanceOf(EmailInvalidoException.class);
            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Rechaza un email ya registrado por otro donante")
        void rechazaEmailDuplicado() {
            when(repositorio.existePorEmail("juan@example.com")).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrar(dtoHumana("juan@example.com")))
                    .isInstanceOf(EmailYaRegistradoException.class);
            verify(repositorio, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("Consulta de donantes")
    class Consulta {

        @Test
        @DisplayName("obtenerDonante() devuelve el donante por id")
        void obtenerPorId() {
            UUID id = UUID.randomUUID();
            when(repositorio.obtenerPersona(id)).thenReturn(humanaGuardada(id));

            assertThat(servicio.obtenerDonante(id).getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("obtenerDonante() lanza 404 si el donante no existe")
        void obtenerPorIdInexistente() {
            UUID id = UUID.randomUUID();
            when(repositorio.obtenerPersona(id)).thenReturn(null);

            assertThatThrownBy(() -> servicio.obtenerDonante(id))
                    .isInstanceOf(PersonaDonanteNoEncontradaException.class);
        }

        @Test
        @DisplayName("Sin filtro de estado devuelve todos los donantes")
        void listarTodos() {
            when(repositorio.obtenerTodosDonantes())
                    .thenReturn(List.of(humanaGuardada(UUID.randomUUID()), juridicaGuardada(UUID.randomUUID())));

            assertThat(servicio.obtenerDonantes(null)).hasSize(2);
        }

        @Test
        @DisplayName("Con filtro de estado delega la búsqueda en el repositorio")
        void listarPorEstado() {
            when(repositorio.obtenerPorEstado("BLOQUEADO")).thenReturn(List.of(humanaGuardada(UUID.randomUUID())));

            assertThat(servicio.obtenerDonantes("BLOQUEADO")).hasSize(1);
            verify(repositorio, never()).obtenerTodosDonantes();
        }

        @Test
        @DisplayName("Un filtro de estado en blanco se trata como si no se hubiera enviado")
        void filtroEnBlancoSeIgnora() {
            when(repositorio.obtenerTodosDonantes()).thenReturn(List.of(humanaGuardada(UUID.randomUUID())));

            assertThat(servicio.obtenerDonantes("   ")).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Cambio de estado del donante")
    class CambioDeEstado {

        @Test
        @DisplayName("Dar de baja a un donante lo deja INACTIVO")
        void darDeBaja() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            servicio.cambiarEstado(id, dtoEstado("INACTIVO", null));

            assertThat(donante.getEstado()).isInstanceOf(InactivoState.class);
        }

        @Test
        @DisplayName("Bloquear con justificación deja al donante BLOQUEADO")
        void bloquearConJustificacion() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            servicio.cambiarEstado(id, dtoEstado("BLOQUEADO", "Reportes de uso indebido"));

            assertThat(donante.getEstado()).isInstanceOf(BloqueadoState.class);
        }

        @Test
        @DisplayName("Bloquear sin justificación se rechaza y el donante sigue ACTIVO")
        void bloquearSinJustificacion() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            assertThatThrownBy(() -> servicio.cambiarEstado(id, dtoEstado("BLOQUEADO", null)))
                    .isInstanceOf(FaltaJustificacionException.class);
            assertThat(donante.getEstado()).isInstanceOf(ActivoState.class);
        }

        @Test
        @DisplayName("Una transición ilegal se rechaza (422)")
        void transicionIlegal() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            donante.cambiarEstado("INACTIVO", null);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            assertThatThrownBy(() -> servicio.cambiarEstado(id, dtoEstado("BLOQUEADO", "motivo")))
                    .isInstanceOf(CambioEstadoPersonaIlegalException.class);
        }

        @Test
        @DisplayName("Cambiar al mismo estado se rechaza (409)")
        void mismoEstado() {
            UUID id = UUID.randomUUID();
            when(repositorio.obtenerPersona(id)).thenReturn(humanaGuardada(id));

            assertThatThrownBy(() -> servicio.cambiarEstado(id, dtoEstado("ACTIVO", null)))
                    .isInstanceOf(PersonaConMismoEstadoException.class);
        }

        @Test
        @DisplayName("Un cambio de estado exitoso registra la interacción del donante")
        void registraInteraccion() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            servicio.cambiarEstado(id, dtoEstado("INACTIVO", null));

            assertThat(donante.getUltimaInteraccion()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Actualización de datos")
    class Actualizacion {

        @Test
        @DisplayName("Actualiza nombre y apellido de una persona humana")
        void actualizaPersonaHumana() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            PersonaDonanteUpdateRequestDTO dto = new PersonaDonanteUpdateRequestDTO();
            dto.setNombre("Juan Carlos");
            dto.setApellido("Pérez García");

            servicio.actualizar(id, dto);

            assertThat(donante.getNombre()).isEqualTo("Juan Carlos");
            assertThat(donante.getApellido()).isEqualTo("Pérez García");
        }

        @Test
        @DisplayName("Actualiza razón social y rubro de una persona jurídica")
        void actualizaPersonaJuridica() {
            UUID id = UUID.randomUUID();
            PersonaJuridica donante = juridicaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            PersonaDonanteUpdateRequestDTO dto = new PersonaDonanteUpdateRequestDTO();
            dto.setRazonSocial("Fundación Renovada");
            dto.setRubro("Indumentaria");

            servicio.actualizar(id, dto);

            assertThat(donante.getRazonSocial()).isEqualTo("Fundación Renovada");
            assertThat(donante.getRubro()).isEqualTo("Indumentaria");
        }

        @Test
        @DisplayName("Los campos no enviados NO se pisan (actualización parcial)")
        void camposNulosNoSePisan() {
            // Comportamiento clave del PUT: mandar solo el apellido no debe borrar
            // el nombre ni la dirección ya cargados.
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            PersonaDonanteUpdateRequestDTO dto = new PersonaDonanteUpdateRequestDTO();
            dto.setApellido("Pérez García");

            servicio.actualizar(id, dto);

            assertThat(donante.getNombre()).isEqualTo("Juan");
            assertThat(donante.getApellido()).isEqualTo("Pérez García");
            assertThat(donante.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 20));
        }

        @Test
        @DisplayName("Los campos de persona jurídica se ignoran al actualizar una persona humana")
        void camposDeOtroSubtipoSeIgnoran() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            PersonaDonanteUpdateRequestDTO dto = new PersonaDonanteUpdateRequestDTO();
            dto.setRazonSocial("Esto no aplica a una persona humana");
            dto.setNombre("Juan Carlos");

            servicio.actualizar(id, dto);

            assertThat(donante.getNombre()).isEqualTo("Juan Carlos");
        }

        @Test
        @DisplayName("Reemplaza la lista de contactos cuando se envía")
        void reemplazaContactos() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            PersonaDonanteUpdateRequestDTO dto = new PersonaDonanteUpdateRequestDTO();
            dto.setContactos(List.of(
                    EmailRequestDTO.builder().valor("nuevo@example.com").build(),
                    TelefonoRequestDTO.builder().valor("1155667788").build()));

            servicio.actualizar(id, dto);

            assertThat(donante.getContactos()).hasSize(2);
        }

        @Test
        @DisplayName("Actualizar registra la interacción y persiste el donante")
        void registraInteraccionYPersiste() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            servicio.actualizar(id, new PersonaDonanteUpdateRequestDTO());

            assertThat(donante.getUltimaInteraccion()).isNotNull();
            verify(repositorio).guardar(donante);
        }

        @Test
        @DisplayName("Lanza 404 si el donante a actualizar no existe")
        void actualizarInexistente() {
            UUID id = UUID.randomUUID();
            when(repositorio.obtenerPersona(id)).thenReturn(null);

            assertThatThrownBy(() -> servicio.actualizar(id, new PersonaDonanteUpdateRequestDTO()))
                    .isInstanceOf(PersonaDonanteNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Contactos y representantes")
    class ContactosYRepresentantes {

        @Test
        @DisplayName("Modificar el contacto reemplaza el del mismo tipo, registra la interacción y PERSISTE")
        void modificaContacto() {
            // El guardado es lo esencial: con JPA la instancia que trae el
            // validador queda detached, así que sin guardar no pasa nada.
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            servicio.modificarContacto(id, EmailRequestDTO.builder().valor("nuevo@example.com").build());

            assertThat(donante.getContactos())
                    .hasSize(1)
                    .allMatch(c -> c.getValor().equals("nuevo@example.com"));
            assertThat(donante.getUltimaInteraccion()).isNotNull();
            verify(repositorio).guardar(donante);
        }

        @Test
        @DisplayName("Un contacto nuevo del mismo tipo reemplaza al anterior en lugar de sumarse")
        void reemplazaElContactoDelMismoTipo() {
            UUID id = UUID.randomUUID();
            PersonaHumana donante = humanaGuardada(id);
            donante.getContactos().add(
                    ar.utn.donatrack.donaciones.models.contacto.Email.builder().valor("viejo@example.com").build());
            when(repositorio.obtenerPersona(id)).thenReturn(donante);

            servicio.modificarContacto(id, EmailRequestDTO.builder().valor("nuevo@example.com").build());

            assertThat(donante.getContactos()).hasSize(1);
            assertThat(donante.getContactos().getFirst().getValor()).isEqualTo("nuevo@example.com");
        }

        @Test
        @DisplayName("Rechaza un contacto con valor en blanco sin guardar nada")
        void rechazaContactoEnBlanco() {
            UUID id = UUID.randomUUID();
            when(repositorio.obtenerPersona(id)).thenReturn(humanaGuardada(id));

            assertThatThrownBy(() -> servicio.modificarContacto(
                    id, EmailRequestDTO.builder().valor("   ").build()))
                    .isInstanceOf(MedioContactoInvalidoException.class);
            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Modificar un representante funciona sobre una persona jurídica")
        void modificaRepresentanteEnJuridica() {
            UUID id = UUID.randomUUID();
            when(repositorio.existePorId(id)).thenReturn(true);
            when(repositorio.obtenerPersona(id)).thenReturn(juridicaGuardada(id));

            servicio.modificarRepresentante(id, RepresentanteRequestDTO.builder()
                    .nombre("Ana").apellido("Gómez").email("ana@fundacion.org").build());

            verify(repositorio).modificarRepresentante(eq(id), any());
        }

        @Test
        @DisplayName("Rechaza modificar representantes de una persona humana")
        void rechazaRepresentanteEnHumana() {
            // Solo las organizaciones tienen representantes; pedirlo sobre una
            // persona humana es un error del cliente, no un caso válido.
            UUID id = UUID.randomUUID();
            when(repositorio.existePorId(id)).thenReturn(true);
            when(repositorio.obtenerPersona(id)).thenReturn(humanaGuardada(id));

            assertThatThrownBy(() -> servicio.modificarRepresentante(id, RepresentanteRequestDTO.builder()
                    .nombre("Ana").apellido("Gómez").email("ana@fundacion.org").build()))
                    .isInstanceOf(TipoPersonaIlegalException.class);
            verify(repositorio, never()).modificarRepresentante(any(), any());
        }
    }

    @Nested
    @DisplayName("Baja de donantes")
    class Baja {

        @Test
        @DisplayName("Elimina al donante después de validar que existe")
        void elimina() {
            UUID id = UUID.randomUUID();
            when(repositorio.existePorId(id)).thenReturn(true);

            servicio.eliminar(id);

            verify(repositorio).eliminar(id);
        }

        @Test
        @DisplayName("Lanza 404 y no borra nada si el donante no existe")
        void eliminarInexistente() {
            UUID id = UUID.randomUUID();
            when(repositorio.existePorId(id)).thenReturn(false);

            assertThatThrownBy(() -> servicio.eliminar(id))
                    .isInstanceOf(PersonaDonanteNoEncontradaException.class);
            verify(repositorio, never()).eliminar(id);
        }
    }
}
