package ar.utn.donatrack.donaciones.validations;

import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CampaniaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.FechasCampaniaInvalidasException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.NecesidadNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.EmailYaRegistradoException;
import ar.utn.donatrack.donaciones.exceptions.mediosContactoExceptions.MedioContactoInvalidoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.TipoPersonaIlegalException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.PersonaJuridica;
import ar.utn.donatrack.donaciones.models.donante.estado.ActivoState;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import ar.utn.donatrack.donaciones.validations.entidades.EntidadesBeneficiariasValidator;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests de los tres validadores del servicio.
 *
 * Los validadores concentran las precondiciones que comparten los services:
 * "existe esta donación", "existe esta entidad", "el email es válido y único".
 * Cada uno lanza una excepción de dominio distinta, y el GlobalExceptionHandler
 * las traduce al código HTTP correspondiente (404, 409, 400, 422).
 *
 * Probarlos por separado permite que los tests de los services se concentren en
 * la lógica de negocio en lugar de repetir estas verificaciones.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Validadores - precondiciones compartidas por los services")
class ValidatorsTest {

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("DonacionesValidator")
    class DonacionesValidatorTest {

        @Mock
        private RepositorioDonaciones repositorio;

        @InjectMocks
        private DonacionesValidator validador;

        @Test
        @DisplayName("validarYObtenerDonacion() devuelve la donación cuando existe")
        void devuelveLaDonacionExistente() {
            UUID id = UUID.randomUUID();
            Donacion donacion = new Donacion();
            when(repositorio.obtenerPorId(id)).thenReturn(donacion);

            assertThat(validador.validarYObtenerDonacion(id)).isSameAs(donacion);
        }

        @Test
        @DisplayName("validarYObtenerDonacion() lanza DonacionNoEncontrada (404) si el id no existe")
        void lanzaSiNoExiste() {
            UUID id = UUID.randomUUID();
            when(repositorio.obtenerPorId(id)).thenReturn(null);

            assertThatThrownBy(() -> validador.validarYObtenerDonacion(id))
                    .isInstanceOf(DonacionNoEncontradaException.class);
        }

    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("PersonasValidator")
    class PersonasValidatorTest {

        @Mock
        private RepositorioPersonasDonantes repositorio;

        @InjectMocks
        private PersonasValidator validador;

        @Nested
        @DisplayName("validarEmail() - formato y unicidad")
        class ValidarEmail {

            @ParameterizedTest(name = "\"{0}\" es un email válido")
            @ValueSource(strings = {
                    "juan@example.com",
                    "juan.perez@example.com.ar",
                    "juan+etiqueta@example.com",
                    "juan_perez@sub.example.com",
                    "j@e.co"
            })
            @DisplayName("Acepta emails con formato válido que aún no estén registrados")
            void aceptaEmailsValidos(String email) {
                when(repositorio.existePorEmail(email)).thenReturn(false);

                assertThatCode(() -> validador.validarEmail(email)).doesNotThrowAnyException();
            }

            @ParameterizedTest(name = "\"{0}\" es rechazado por formato")
            @ValueSource(strings = {
                    "sin-arroba.com",
                    "@sindestinatario.com",
                    "con espacio@example.com",
                    "juan@",
                    ""
            })
            @DisplayName("Rechaza emails con formato inválido (400)")
            void rechazaEmailsInvalidos(String email) {
                // El email es el canal obligatorio del donante y la clave de
                // idempotencia del CSV: no puede quedar mal cargado.
                assertThatThrownBy(() -> validador.validarEmail(email))
                        .isInstanceOf(EmailInvalidoException.class);
            }

            @Test
            @DisplayName("Rechaza un email nulo")
            void rechazaEmailNulo() {
                assertThatThrownBy(() -> validador.validarEmail(null))
                        .isInstanceOf(EmailInvalidoException.class);
            }

            @Test
            @DisplayName("Rechaza un email ya registrado por otro donante (409)")
            void rechazaEmailDuplicado() {
                // Sin esta regla, dos donantes compartirían clave y la importación
                // CSV no podría decidir a cuál actualizar.
                when(repositorio.existePorEmail("juan@example.com")).thenReturn(true);

                assertThatThrownBy(() -> validador.validarEmail("juan@example.com"))
                        .isInstanceOf(EmailYaRegistradoException.class);
            }

            @ParameterizedTest(name = "\"{0}\" es un email válido")
            @ValueSource(strings = {
                    "benjamíncastro8929@yahoo.com",
                    "sofíagarcía1520@gmail.com",
                    "muñoz@mail.com"
            })
            @DisplayName("Acepta emails con tilde o ñ, como los del CSV de prueba del enunciado")
            void aceptaEmailsConTilde(String email) {
                when(repositorio.existePorEmail(email)).thenReturn(false);

                assertThatCode(() -> validador.validarEmail(email)).doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Detecta un duplicado aunque difiera en mayúsculas o espacios (409)")
            void detectaDuplicadoConOtrasMayusculas() {
                // El repositorio guarda y busca la forma canónica: el validador
                // tiene que consultar con esa misma forma.
                when(repositorio.existePorEmail("juan@example.com")).thenReturn(true);

                assertThatThrownBy(() -> validador.validarEmail("  Juan@Example.COM "))
                        .isInstanceOf(EmailYaRegistradoException.class);
            }
        }

        @Nested
        @DisplayName("Existencia y tipo de persona")
        class ExistenciaYTipo {

            @Test
            @DisplayName("validarExistenciaPersona() pasa si el donante existe")
            void existenciaOk() {
                UUID id = UUID.randomUUID();
                when(repositorio.existePorId(id)).thenReturn(true);

                assertThatCode(() -> validador.validarExistenciaPersona(id)).doesNotThrowAnyException();
            }

            @Test
            @DisplayName("validarExistenciaPersona() lanza PersonaDonanteNoEncontrada (404) si no existe")
            void existenciaFalla() {
                UUID id = UUID.randomUUID();
                when(repositorio.existePorId(id)).thenReturn(false);

                assertThatThrownBy(() -> validador.validarExistenciaPersona(id))
                        .isInstanceOf(PersonaDonanteNoEncontradaException.class);
            }

            @Test
            @DisplayName("validarYObtenerPersona() devuelve el donante cuando existe")
            void obtenerPersonaOk() {
                UUID id = UUID.randomUUID();
                PersonaHumana persona = PersonaHumana.builder().id(id).estado(new ActivoState()).build();
                when(repositorio.obtenerPersona(id)).thenReturn(persona);

                assertThat(validador.validarYObtenerPersona(id)).isSameAs(persona);
            }

            @Test
            @DisplayName("validarEsPersonaJuridica() pasa cuando el donante es una organización")
            void esJuridicaOk() {
                // Solo las personas jurídicas tienen representantes, así que este
                // chequeo protege ese endpoint.
                UUID id = UUID.randomUUID();
                when(repositorio.obtenerPersona(id))
                        .thenReturn(PersonaJuridica.builder().id(id).estado(new ActivoState()).build());

                assertThatCode(() -> validador.validarEsPersonaJuridica(id)).doesNotThrowAnyException();
            }

            @Test
            @DisplayName("validarEsPersonaJuridica() lanza TipoPersonaIlegal si el donante es una persona humana")
            void esJuridicaFallaConHumana() {
                UUID id = UUID.randomUUID();
                when(repositorio.obtenerPersona(id))
                        .thenReturn(PersonaHumana.builder().id(id).estado(new ActivoState()).build());

                assertThatThrownBy(() -> validador.validarEsPersonaJuridica(id))
                        .isInstanceOf(TipoPersonaIlegalException.class);
            }
        }

        @Nested
        @DisplayName("validarMedioContacto()")
        class ValidarMedioContacto {

            @Test
            @DisplayName("Acepta un medio de contacto con valor cargado")
            void aceptaMedioValido() {
                assertThatCode(() -> validador.validarMedioContacto(
                        Email.builder().valor("juan@example.com").build()))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("Rechaza un medio nulo")
            void rechazaMedioNulo() {
                assertThatThrownBy(() -> validador.validarMedioContacto(null))
                        .isInstanceOf(MedioContactoInvalidoException.class);
            }

            @Test
            @DisplayName("Rechaza un medio con valor nulo o en blanco")
            void rechazaValorVacio() {
                // Un contacto sin valor no sirve para notificar: es peor que no tenerlo,
                // porque el sistema creería que puede contactar al donante.
                assertThatThrownBy(() -> validador.validarMedioContacto(Email.builder().valor(null).build()))
                        .isInstanceOf(MedioContactoInvalidoException.class);
                assertThatThrownBy(() -> validador.validarMedioContacto(Email.builder().valor("  ").build()))
                        .isInstanceOf(MedioContactoInvalidoException.class);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("EntidadesBeneficiariasValidator")
    class EntidadesBeneficiariasValidatorTest {

        @Mock
        private RepositorioEntidadesBeneficiarias repositorio;

        @InjectMocks
        private EntidadesBeneficiariasValidator validador;

        /** Entidad con una campaña ya cargada, para probar la navegación en cascada. */
        private EntidadBeneficiaria entidadConCampania(Campania campania) {
            EntidadBeneficiaria entidad = EntidadBeneficiaria.builder()
                    .id(UUID.randomUUID())
                    .razonSocial("Comedor Los Pibes")
                    .campanias(new ArrayList<>())
                    .build();
            entidad.agregarCampania(campania);
            return entidad;
        }

        private Campania campania() {
            Campania campania = new Campania();
            campania.setIdCampania(UUID.randomUUID());
            return campania;
        }

        @Test
        @DisplayName("validarYObtenerEntidad() devuelve la entidad cuando existe")
        void obtenerEntidadOk() {
            UUID id = UUID.randomUUID();
            EntidadBeneficiaria entidad = EntidadBeneficiaria.builder().id(id).build();
            when(repositorio.obtenerPorId(id)).thenReturn(entidad);

            assertThat(validador.validarYObtenerEntidad(id)).isSameAs(entidad);
        }

        @Test
        @DisplayName("validarYObtenerEntidad() lanza EntidadBeneficiariaNoEncontrada (404) si no existe")
        void obtenerEntidadFalla() {
            UUID id = UUID.randomUUID();
            when(repositorio.obtenerPorId(id)).thenReturn(null);

            assertThatThrownBy(() -> validador.validarYObtenerEntidad(id))
                    .isInstanceOf(EntidadBeneficiariaNoEncontradaException.class);
        }

        @Test
        @DisplayName("validarExistenciaEntidad() lanza si el id no está registrado")
        void existenciaFalla() {
            UUID id = UUID.randomUUID();
            when(repositorio.existePorId(id)).thenReturn(false);

            assertThatThrownBy(() -> validador.validarExistenciaEntidad(id))
                    .isInstanceOf(EntidadBeneficiariaNoEncontradaException.class);
        }

        @Test
        @DisplayName("validarYObtenerCampania() encuentra la campaña dentro de la entidad")
        void obtenerCampaniaOk() {
            Campania campania = campania();
            EntidadBeneficiaria entidad = entidadConCampania(campania);

            assertThat(validador.validarYObtenerCampania(entidad, campania.getIdCampania())).isSameAs(campania);
        }

        @Test
        @DisplayName("validarYObtenerCampania() lanza CampaniaNoEncontrada si no pertenece a la entidad")
        void obtenerCampaniaFalla() {
            // Recorrido en cascada entidad -> campaña -> necesidad: cada nivel debe
            // fallar con su propio 404 para que el mensaje al cliente sea claro.
            EntidadBeneficiaria entidad = entidadConCampania(campania());

            assertThatThrownBy(() -> validador.validarYObtenerCampania(entidad, UUID.randomUUID()))
                    .isInstanceOf(CampaniaNoEncontradaException.class);
        }

        @Test
        @DisplayName("validarYObtenerNecesidad() encuentra la necesidad dentro de la campaña")
        void obtenerNecesidadOk() {
            Campania campania = campania();
            Necesidad necesidad = new NecesidadExtraordinaria();
            necesidad.setNombre("Arroz");
            campania.agregarNecesidad(necesidad);

            assertThat(validador.validarYObtenerNecesidad(campania, necesidad.getId())).isSameAs(necesidad);
        }

        @Test
        @DisplayName("validarYObtenerNecesidad() lanza NecesidadNoEncontrada si no pertenece a la campaña")
        void obtenerNecesidadFalla() {
            assertThatThrownBy(() -> validador.validarYObtenerNecesidad(campania(), UUID.randomUUID()))
                    .isInstanceOf(NecesidadNoEncontradaException.class);
        }

        @Test
        @DisplayName("validarFechasCampania() acepta un rango donde el inicio es anterior al fin")
        void fechasValidas() {
            assertThatCode(() -> validador.validarFechasCampania(
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validarFechasCampania() acepta que inicio y fin sean el mismo día")
        void mismoDiaEsValido() {
            // Caso borde: una campaña relámpago de un solo día es legítima.
            LocalDate mismoDia = LocalDate.of(2026, 3, 1);

            assertThatCode(() -> validador.validarFechasCampania(mismoDia, mismoDia))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validarFechasCampania() rechaza un inicio posterior al fin")
        void fechasInvertidas() {
            assertThatThrownBy(() -> validador.validarFechasCampania(
                    LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 1)))
                    .isInstanceOf(FechasCampaniaInvalidasException.class);
        }

        @Test
        @DisplayName("validarFechasCampania() no valida nada si alguna fecha es nula")
        void fechasNulasSeIgnoran() {
            // Las fechas nulas ya las rechaza la validación del DTO (@NotNull);
            // acá solo se comprueba que el validador no explote.
            assertThatCode(() -> validador.validarFechasCampania(null, LocalDate.of(2026, 3, 1)))
                    .doesNotThrowAnyException();
            assertThatCode(() -> validador.validarFechasCampania(LocalDate.of(2026, 3, 1), null))
                    .doesNotThrowAnyException();
        }
    }
}
