package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadConDonacionesException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.dtos.request.CampaniaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.NecesidadExtraordinariaRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.NecesidadRecurrenteRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CampaniaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadRecurrenteResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.NecesidadResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CambioTipoNecesidadException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CampaniaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.FechasCampaniaInvalidasException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.NecesidadNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.mappers.PersonaDonanteMapper;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.validations.entidades.EntidadesBeneficiariasValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del CRUD de entidades beneficiarias, campañas y necesidades.
 *
 * Cubre el requisito del enunciado de que una entidad pueda declarar qué
 * necesita, agrupado en campañas, y que ese catálogo se pueda consultar,
 * modificar y dar de baja. Es el insumo que después usa el algoritmo de
 * asignación por compatibilidad semántica.
 *
 * Se usan el validador y el mapper REALES (son objetos puros, sin estado) y se
 * mockea solo el repositorio: así los tests ejercitan también las reglas del
 * mapper, como el rechazo del cambio de tipo de necesidad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntidadesBeneficiariasService - CRUD de entidades, campañas y necesidades")
class EntidadesBeneficiariasServiceTest {

    @Mock
    private RepositorioEntidadesBeneficiarias repositorio;

    @Mock
    private RepositorioDonaciones donaciones;

    private EntidadesBeneficiariasService servicio;

    private EntidadBeneficiaria entidad;
    private Campania campania;

    @BeforeEach
    void prepararEscenario() {
        PersonaDonanteMapper dependenciasMapper = new PersonaDonanteMapper();
        EntidadBeneficiariaMapper mapper = new EntidadBeneficiariaMapper(dependenciasMapper);
        EntidadesBeneficiariasValidator validador = new EntidadesBeneficiariasValidator(repositorio);

        servicio = new EntidadesBeneficiariasService(repositorio, donaciones, validador, mapper, dependenciasMapper);

        entidad = EntidadBeneficiaria.builder()
                .id(UUID.randomUUID())
                .razonSocial("Comedor Los Pibes")
                .contactos(new ArrayList<>())
                .representantes(new ArrayList<>())
                .campanias(new ArrayList<>())
                .build();

        campania = new Campania();
        campania.setIdCampania(UUID.randomUUID());
        campania.setIdEntidad(entidad.getId());
        campania.setDescripcionGeneral("Colecta post inundación");
        campania.setFechaInicio(LocalDate.now());
        campania.setFechaFin(LocalDate.now().plusMonths(1));
        campania.setNecesidades(new ArrayList<>());
        entidad.agregarCampania(campania);
    }

    private NecesidadExtraordinariaRequestDTO dtoExtraordinaria(String nombre, int objetivo) {
        return NecesidadExtraordinariaRequestDTO.builder()
                .nombre(nombre)
                .descripcion("Descripción de " + nombre)
                .cantidadObjetivo(objetivo)
                .build();
    }

    private NecesidadRecurrenteRequestDTO dtoRecurrente(String nombre, int objetivo, Periodicidad periodo) {
        return NecesidadRecurrenteRequestDTO.builder()
                .nombre(nombre)
                .descripcion("Descripción de " + nombre)
                .cantidadObjetivo(objetivo)
                .periodo(periodo)
                .build();
    }

    /** Agrega directamente al modelo una necesidad extraordinaria ya existente. */
    private Necesidad necesidadYaCargada(String nombre) {
        NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria();
        necesidad.setNombre(nombre);
        necesidad.setCantidadObjetivo(100);
        campania.agregarNecesidad(necesidad);
        return necesidad;
    }

    @Nested
    @DisplayName("Consulta y baja de entidades")
    class ConsultaYBaja {

        @Test
        @DisplayName("obtenerPorId() devuelve la entidad con sus campañas")
        void obtenerPorId() {
            when(repositorio.existePorId(entidad.getId())).thenReturn(true);
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            assertThat(servicio.obtenerPorId(entidad.getId()).getRazonSocial()).isEqualTo("Comedor Los Pibes");
        }

        @Test
        @DisplayName("obtenerPorId() lanza 404 si la entidad no existe")
        void obtenerPorIdInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.existePorId(idInexistente)).thenReturn(false);

            assertThatThrownBy(() -> servicio.obtenerPorId(idInexistente))
                    .isInstanceOf(EntidadBeneficiariaNoEncontradaException.class);
        }

        @Test
        @DisplayName("obtenerTodas() devuelve todas las entidades registradas")
        void obtenerTodas() {
            when(repositorio.buscarTodas()).thenReturn(List.of(entidad));

            assertThat(servicio.obtenerTodas()).hasSize(1);
        }

        @Test
        @DisplayName("eliminarEntidad() valida que exista antes de borrar")
        void eliminarEntidad() {
            when(repositorio.existePorId(entidad.getId())).thenReturn(true);

            servicio.eliminarEntidad(entidad.getId());

            verify(repositorio).eliminar(entidad.getId());
        }

        @Test
        @DisplayName("eliminarEntidad() no borra nada si la entidad no existe")
        void eliminarEntidadInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.existePorId(idInexistente)).thenReturn(false);

            assertThatThrownBy(() -> servicio.eliminarEntidad(idInexistente))
                    .isInstanceOf(EntidadBeneficiariaNoEncontradaException.class);
            verify(repositorio, never()).eliminar(idInexistente);
        }

        @Test
        @DisplayName("eliminarEntidad() rechaza el borrado (409) si la entidad tiene donaciones asignadas")
        void eliminarEntidadConDonaciones() {
            // Entrega 5: donacion.id_entidad_beneficiaria no es FK; si se borrara
            // la entidad, sus donaciones quedarían apuntando a nadie.
            when(repositorio.existePorId(entidad.getId())).thenReturn(true);
            when(donaciones.existenAsignadasA(entidad.getId())).thenReturn(true);

            assertThatThrownBy(() -> servicio.eliminarEntidad(entidad.getId()))
                    .isInstanceOf(EntidadConDonacionesException.class);
            verify(repositorio, never()).eliminar(entidad.getId());
        }
    }

    @Nested
    @DisplayName("Alta de campañas")
    class AltaDeCampanias {

        @Test
        @DisplayName("Agrega la campaña a la entidad y devuelve el DTO con su id generado")
        void agregarCampania() {
            when(repositorio.existePorId(entidad.getId())).thenReturn(true);
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            CampaniaRequestDTO dto = CampaniaRequestDTO.builder()
                    .descripcionGeneral("Campaña de invierno")
                    .fechaInicio(LocalDate.now())
                    .fechaFin(LocalDate.now().plusMonths(2))
                    .build();

            CampaniaResponseDTO respuesta = servicio.agregarCampaniaAEntidad(entidad.getId(), dto);

            assertThat(respuesta.getIdCampania()).isNotNull();
            assertThat(respuesta.getIdEntidad()).isEqualTo(entidad.getId());
            assertThat(entidad.getCampanias()).hasSize(2);
            verify(repositorio).guardar(entidad);
        }

        @Test
        @DisplayName("Rechaza una campaña cuya fecha de inicio es posterior a la de fin")
        void rechazaFechasInvertidas() {
            // Regla de coherencia temporal: una campaña no puede terminar antes de empezar.
            when(repositorio.existePorId(entidad.getId())).thenReturn(true);

            CampaniaRequestDTO dto = CampaniaRequestDTO.builder()
                    .descripcionGeneral("Campaña mal cargada")
                    .fechaInicio(LocalDate.now().plusMonths(2))
                    .fechaFin(LocalDate.now().plusMonths(1))
                    .build();

            assertThatThrownBy(() -> servicio.agregarCampaniaAEntidad(entidad.getId(), dto))
                    .isInstanceOf(FechasCampaniaInvalidasException.class);
            verify(repositorio, never()).guardar(entidad);
        }
    }

    @Nested
    @DisplayName("Alta de necesidades dentro de una campaña")
    class AltaDeNecesidades {

        @Test
        @DisplayName("Agrega una necesidad extraordinaria y la deja con 0 recibido")
        void agregarExtraordinaria() {
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            NecesidadResponseDTO respuesta = servicio.agregarNecesidadACampania(
                    entidad.getId(), campania.getIdCampania(), dtoExtraordinaria("Colchones", 50));

            assertThat(respuesta.getId()).isNotNull();
            assertThat(respuesta.getCantidadRecibida()).isZero();
            assertThat(respuesta.isSatisfecha()).isFalse();
            assertThat(campania.getNecesidades()).hasSize(1);
        }

        @Test
        @DisplayName("Agrega una necesidad recurrente con su periodicidad y el período arrancando hoy")
        void agregarRecurrente() {
            // El período debe arrancar en el alta: si quedara nulo, periodoVencido()
            // devolvería siempre false y la necesidad nunca se reiniciaría.
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            NecesidadResponseDTO respuesta = servicio.agregarNecesidadACampania(
                    entidad.getId(), campania.getIdCampania(), dtoRecurrente("Fideos", 100, Periodicidad.SEMANAL));

            assertThat(respuesta).isInstanceOf(NecesidadRecurrenteResponseDTO.class);
            NecesidadRecurrenteResponseDTO recurrente = (NecesidadRecurrenteResponseDTO) respuesta;
            assertThat(recurrente.getPeriodo()).isEqualTo(Periodicidad.SEMANAL);
            assertThat(recurrente.getFechaInicioPeriodo()).isEqualTo(FechaHoraArgentina.hoy());
        }

        @Test
        @DisplayName("Lanza 404 si la campaña no pertenece a la entidad")
        void campaniaInexistente() {
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            assertThatThrownBy(() -> servicio.agregarNecesidadACampania(
                    entidad.getId(), UUID.randomUUID(), dtoExtraordinaria("Colchones", 50)))
                    .isInstanceOf(CampaniaNoEncontradaException.class);
        }

        @Test
        @DisplayName("Lanza 404 si la entidad no existe")
        void entidadInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.obtenerPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.agregarNecesidadACampania(
                    idInexistente, campania.getIdCampania(), dtoExtraordinaria("Colchones", 50)))
                    .isInstanceOf(EntidadBeneficiariaNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Consulta de necesidades")
    class ConsultaDeNecesidades {

        @Test
        @DisplayName("obtenerNecesidadesDeCampania() devuelve todas las necesidades cargadas")
        void listarNecesidades() {
            necesidadYaCargada("Arroz");
            necesidadYaCargada("Colchones");
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            assertThat(servicio.obtenerNecesidadesDeCampania(entidad.getId(), campania.getIdCampania()))
                    .hasSize(2)
                    .extracting(NecesidadResponseDTO::getNombre)
                    .containsExactly("Arroz", "Colchones");
        }

        @Test
        @DisplayName("obtenerNecesidad() devuelve una necesidad puntual por id")
        void obtenerUnaNecesidad() {
            Necesidad necesidad = necesidadYaCargada("Arroz");
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            assertThat(servicio.obtenerNecesidad(entidad.getId(), campania.getIdCampania(), necesidad.getId())
                    .getNombre()).isEqualTo("Arroz");
        }

        @Test
        @DisplayName("obtenerNecesidad() lanza 404 si la necesidad no está en esa campaña")
        void necesidadInexistente() {
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            assertThatThrownBy(() -> servicio.obtenerNecesidad(
                    entidad.getId(), campania.getIdCampania(), UUID.randomUUID()))
                    .isInstanceOf(NecesidadNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Modificación de necesidades")
    class ModificacionDeNecesidades {

        @Test
        @DisplayName("Actualiza nombre, descripción y cantidad objetivo de una necesidad existente")
        void actualizaCamposComunes() {
            Necesidad necesidad = necesidadYaCargada("Arroz");
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            servicio.actualizarNecesidad(entidad.getId(), campania.getIdCampania(), necesidad.getId(),
                    dtoExtraordinaria("Arroz integral", 250));

            assertThat(necesidad.getNombre()).isEqualTo("Arroz integral");
            assertThat(necesidad.getCantidadObjetivo()).isEqualTo(250);
            verify(repositorio).guardar(entidad);
        }

        @Test
        @DisplayName("Actualizar NO reinicia lo ya recibido: eso lo registra el flujo de donaciones")
        void noPisaLaCantidadRecibida() {
            // Si una edición del catálogo borrara lo recibido, se perdería el
            // avance real de la necesidad.
            Necesidad necesidad = necesidadYaCargada("Arroz");
            necesidad.recibirDonacion(40);
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            servicio.actualizarNecesidad(entidad.getId(), campania.getIdCampania(), necesidad.getId(),
                    dtoExtraordinaria("Arroz", 300));

            assertThat(necesidad.getCantidadRecibida()).isEqualTo(40);
        }

        @Test
        @DisplayName("Actualizar una recurrente cambia la periodicidad pero NO reinicia el período en curso")
        void actualizaPeriodicidadSinReiniciarPeriodo() {
            NecesidadRecurrente recurrente = new NecesidadRecurrente();
            recurrente.setNombre("Fideos");
            recurrente.setCantidadObjetivo(100);
            recurrente.setPeriodo(Periodicidad.SEMANAL);
            LocalDate inicioOriginal = LocalDate.now().minusDays(3);
            recurrente.setFechaInicioPeriodo(inicioOriginal);
            campania.agregarNecesidad(recurrente);
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            servicio.actualizarNecesidad(entidad.getId(), campania.getIdCampania(), recurrente.getId(),
                    dtoRecurrente("Fideos", 100, Periodicidad.MENSUAL));

            assertThat(recurrente.getPeriodo()).isEqualTo(Periodicidad.MENSUAL);
            assertThat(recurrente.getFechaInicioPeriodo()).isEqualTo(inicioOriginal);
        }

        @Test
        @DisplayName("RECHAZA convertir una necesidad extraordinaria en recurrente")
        void rechazaCambioDeTipo() {
            // Cambiar el tipo cambiaría el significado de lo ya recibido
            // (una recurrente se reinicia por período y una extraordinaria no),
            // así que se obliga a eliminar y volver a crear.
            Necesidad extraordinaria = necesidadYaCargada("Colchones");
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            assertThatThrownBy(() -> servicio.actualizarNecesidad(
                    entidad.getId(), campania.getIdCampania(), extraordinaria.getId(),
                    dtoRecurrente("Colchones", 100, Periodicidad.MENSUAL)))
                    .isInstanceOf(CambioTipoNecesidadException.class);
        }

        @Test
        @DisplayName("RECHAZA convertir una necesidad recurrente en extraordinaria")
        void rechazaCambioDeTipoInverso() {
            NecesidadRecurrente recurrente = new NecesidadRecurrente();
            recurrente.setNombre("Fideos");
            recurrente.setCantidadObjetivo(100);
            recurrente.setPeriodo(Periodicidad.SEMANAL);
            campania.agregarNecesidad(recurrente);
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            assertThatThrownBy(() -> servicio.actualizarNecesidad(
                    entidad.getId(), campania.getIdCampania(), recurrente.getId(),
                    dtoExtraordinaria("Fideos", 100)))
                    .isInstanceOf(CambioTipoNecesidadException.class);
        }
    }

    @Nested
    @DisplayName("Baja de necesidades")
    class BajaDeNecesidades {

        @Test
        @DisplayName("Elimina la necesidad de la campaña y persiste la entidad")
        void eliminaLaNecesidad() {
            Necesidad necesidad = necesidadYaCargada("Arroz");
            necesidadYaCargada("Colchones");
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            servicio.eliminarNecesidad(entidad.getId(), campania.getIdCampania(), necesidad.getId());

            assertThat(campania.getNecesidades()).hasSize(1);
            verify(repositorio).guardar(entidad);
        }

        @Test
        @DisplayName("Lanza 404 en lugar de borrar en silencio cuando la necesidad no existe")
        void necesidadInexistenteLanza404() {
            // El servicio valida ANTES de eliminar justamente para poder responder
            // 404 y no un 204 que haría creer al cliente que borró algo.
            necesidadYaCargada("Arroz");
            when(repositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);

            assertThatThrownBy(() -> servicio.eliminarNecesidad(
                    entidad.getId(), campania.getIdCampania(), UUID.randomUUID()))
                    .isInstanceOf(NecesidadNoEncontradaException.class);
            assertThat(campania.getNecesidades()).hasSize(1);
        }
    }
}
