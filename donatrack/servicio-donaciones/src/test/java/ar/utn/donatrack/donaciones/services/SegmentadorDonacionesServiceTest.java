package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionesRegistradas;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.DonanteBloqueadoException;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.request.DonacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests del servicio que registra una carga de donación.
 *
 * Su responsabilidad es de orquestación, no de reglas de negocio: delega la
 * segmentación en el modelo (CargaDonacion.segmentar(), probado en
 * CargaDonacionTest), persiste el resultado y avisa al servicio de incentivos
 * para que actualice métricas y misiones del donante.
 *
 * Lo que se verifica acá es justamente esa coordinación: que se persista lo
 * segmentado, que se notifique con los datos correctos y —muy importante— que
 * la notificación se omita sin romper cuando falta el donante o su email.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SegmentadorDonacionesService - registro de una carga de donación")
class SegmentadorDonacionesServiceTest {

    @Mock
    private RepositorioDonaciones donacionesRepository;

    @Mock
    private RepositorioPersonasDonantes donanteRepository;

    @Mock
    private ApplicationEventPublisher eventos;

    @Mock
    private PersonasValidator personasValidator;

    @Spy
    private DonacionMapper mapper = new DonacionMapper();

    @InjectMocks
    private SegmentadorDonacionesService servicio;

    @Captor
    private ArgumentCaptor<List<Donacion>> donacionesCaptor;

    @Captor
    private ArgumentCaptor<DonacionesRegistradas> eventoCaptor;

    private final UUID idDonante = UUID.randomUUID();

    private Bien bien(String subcategoria, String descripcion) {
        return BienGenerico.builder()
                .subcategoria(new Subcategoria(subcategoria))
                .descripcion(descripcion)
                .cantidad(1)
                .unidad("unidades")
                .build();
    }

    private PersonaDonante donanteConEmail(String email) {
        return PersonaHumana.builder()
                .id(idDonante)
                .nombre("Juan")
                .email(email)
                .build();
    }

    @Nested
    @DisplayName("Segmentación y persistencia")
    class SegmentacionYPersistencia {

        @Test
        @DisplayName("Devuelve las donaciones segmentadas a partir de la carga")
        void devuelveLasDonacionesSegmentadas() {
            CargaDonacion carga = new CargaDonacion(idDonante, "Colecta",
                    List.of(bien("arroz", "Arroz"), bien("ropa", "Camperas")));
            when(donanteRepository.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));

            List<Donacion> resultado = servicio.segmentar(carga);

            assertThat(resultado)
                    .hasSize(2)
                    .extracting(d -> d.getSubcategoria().getTipo())
                    .containsExactlyInAnyOrder("arroz", "ropa");
        }

        @Test
        @DisplayName("Persiste exactamente las donaciones que devolvió la segmentación")
        void persisteLoSegmentado() {
            // Verificación clave: el repositorio recibe las mismas instancias que
            // se devuelven al controller, no una copia distinta.
            CargaDonacion carga = new CargaDonacion(idDonante, "Colecta",
                    List.of(bien("arroz", "Arroz"), bien("ropa", "Camperas")));
            when(donanteRepository.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));

            List<Donacion> resultado = servicio.segmentar(carga);

            verify(donacionesRepository).cargarDonaciones(donacionesCaptor.capture());
            assertThat(donacionesCaptor.getValue()).isEqualTo(resultado);
        }

        @Test
        @DisplayName("Una carga sin bienes no persiste nada ni falla")
        void cargaVaciaNoRompe() {
            CargaDonacion carga = new CargaDonacion(idDonante, "Colecta vacía", List.of());
            when(donanteRepository.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));

            List<Donacion> resultado = servicio.segmentar(carga);

            assertThat(resultado).isEmpty();
            verify(donacionesRepository).cargarDonaciones(List.of());
        }
    }

    @Nested
    @DisplayName("Aviso a incentivos (evento DonacionesRegistradas)")
    class AvisoAIncentivos {

        @Test
        @DisplayName("Publica el evento con la persona donante, la cantidad de bienes y las categorías")
        void publicaElEvento() {
            // Incentivos usa estos datos para actualizar métricas y verificar si
            // el donante completó alguna misión. El aviso lo manda AvisosOyente
            // después de confirmar.
            CargaDonacion carga = new CargaDonacion(idDonante, "Colecta",
                    List.of(bien("arroz", "Arroz"), bien("ropa", "Camperas")));
            when(donanteRepository.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));

            servicio.segmentar(carga);

            verify(eventos).publishEvent(eventoCaptor.capture());
            DonacionesRegistradas evento = eventoCaptor.getValue();
            assertThat(evento.idDonante()).isEqualTo(idDonante);
            assertThat(evento.cantidadBienes()).isEqualTo(2);
            assertThat(evento.categorias()).containsExactlyInAnyOrder("arroz", "ropa");
            assertThat(evento.cuando()).isNotNull();
        }

        @Test
        @DisplayName("Las categorías no se repiten aunque haya varios bienes de la misma subcategoría")
        void categoriasSinDuplicados() {
            CargaDonacion carga = new CargaDonacion(idDonante, "Colecta",
                    List.of(bien("arroz", "Arroz A"), bien("arroz", "Arroz B"), bien("ropa", "Camperas")));
            when(donanteRepository.buscarPorId(idDonante)).thenReturn(Optional.of(donanteConEmail("juan@example.com")));

            servicio.segmentar(carga);

            // 3 bienes en total, pero solo 2 categorías distintas.
            verify(eventos).publishEvent(eventoCaptor.capture());
            assertThat(eventoCaptor.getValue().cantidadBienes()).isEqualTo(3);
            assertThat(eventoCaptor.getValue().categorias()).containsExactlyInAnyOrder("arroz", "ropa");
        }

        @Test
        @DisplayName("Si el donante no existe, la donación se registra igual y no hay a quién avisar")
        void donanteInexistenteNoRompe() {
            // El aviso a incentivos es secundario: nunca debe hacer fallar el
            // registro de una donación que ya se persistió.
            CargaDonacion carga = new CargaDonacion(idDonante, "Colecta", List.of(bien("arroz", "Arroz")));
            when(donanteRepository.buscarPorId(idDonante)).thenReturn(Optional.empty());

            List<Donacion> resultado = servicio.segmentar(carga);

            assertThat(resultado).hasSize(1);
            verify(donacionesRepository).cargarDonaciones(anyList());
            verifyNoInteractions(eventos);
        }
    }

    @Nested
    @DisplayName("registrar(): el caso de uso completo desde la API")
    class Registrar {

        private DonacionRequestDTO pedido() {
            BienRequestDTO bien = new BienRequestDTO();
            bien.setSubcategoria("arroz");
            bien.setCantidad(3);
            bien.setUnidad("kg");
            return new DonacionRequestDTO(idDonante, "Colecta", List.of(bien));
        }

        private PersonaHumana donante(String estado) {
            PersonaHumana persona = PersonaHumana.builder().id(idDonante).email("ana@mail.com").build();
            if (!"ACTIVO".equals(estado)) {
                persona.cambiarEstado(estado, "motivo");
            }
            return persona;
        }

        @Test
        @DisplayName("Segmenta, guarda y devuelve las donaciones creadas")
        void registraLaCarga() {
            when(personasValidator.validarYObtenerPersona(idDonante)).thenReturn(donante("ACTIVO"));

            List<DonacionResponseDTO> creadas = servicio.registrar(pedido());

            assertThat(creadas).singleElement().satisfies(d -> {
                assertThat(d.getEstado()).isEqualTo("EN_DEPOSITO");
                assertThat(d.getSubcategoria()).isEqualTo("arroz");
            });
            verify(donacionesRepository).cargarDonaciones(anyList());
            // Donar es interactuar con la plataforma: reinicia el plazo de inactividad.
            verify(donanteRepository).guardar(argThat(persona -> persona.getUltimaInteraccion() != null));
            // El donante ya está cargado: no se vuelve a buscar para avisar a incentivos.
            verify(donanteRepository, never()).buscarPorId(any());
        }

        @Test
        @DisplayName("Una persona donante BLOQUEADA no puede registrar donaciones")
        void donanteBloqueado() {
            when(personasValidator.validarYObtenerPersona(idDonante)).thenReturn(donante("BLOQUEADO"));

            assertThatThrownBy(() -> servicio.registrar(pedido()))
                    .isInstanceOf(DonanteBloqueadoException.class);
            verify(donacionesRepository, never()).cargarDonaciones(any());
            verifyNoInteractions(eventos);
        }
    }
}
