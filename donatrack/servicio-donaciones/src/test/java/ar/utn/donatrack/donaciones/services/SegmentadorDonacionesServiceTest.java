package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.DonacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.DonanteBloqueadoException;
import ar.utn.donatrack.donaciones.exceptions.personasExceptions.PersonaDonanteNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioPersonasDonantes;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionesRegistradas;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.validations.personas.PersonasValidator;
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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests del servicio que registra una carga de donación.
 *
 * Su responsabilidad es de orquestación, no de reglas de negocio: valida a la
 * persona donante, delega la segmentación en el modelo (CargaDonacion.segmentar(),
 * probado en CargaDonacionTest), persiste el resultado, registra la interacción
 * y publica el evento con el que AvisosOyente le avisa a incentivos.
 *
 * Lo que se verifica acá es justamente esa coordinación, a través de registrar(),
 * que es el único caso de uso del servicio.
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

    /** Un pedido con un bien genérico por cada subcategoría indicada. */
    private DonacionRequestDTO pedido(String... subcategorias) {
        List<BienRequestDTO> bienes = Arrays.stream(subcategorias).map(subcategoria -> {
            BienRequestDTO bien = new BienRequestDTO();
            bien.setSubcategoria(subcategoria);
            bien.setCantidad(3);
            bien.setUnidad("kg");
            return bien;
        }).toList();
        return new DonacionRequestDTO(idDonante, "Colecta", bienes);
    }

    private PersonaHumana donante(String estado) {
        PersonaHumana persona = PersonaHumana.builder().id(idDonante).email("ana@mail.com").build();
        if (!"ACTIVO".equals(estado)) {
            persona.cambiarEstado(estado, "motivo");
        }
        return persona;
    }

    private void donanteActivo() {
        when(personasValidator.validarYObtenerPersona(idDonante)).thenReturn(donante("ACTIVO"));
    }

    @Nested
    @DisplayName("Segmentación y persistencia")
    class SegmentacionYPersistencia {

        @Test
        @DisplayName("Segmenta, guarda y devuelve las donaciones creadas, una por subcategoría")
        void registraLaCarga() {
            donanteActivo();

            List<DonacionResponseDTO> creadas = servicio.registrar(pedido("arroz", "ropa"));

            assertThat(creadas).hasSize(2)
                    .allSatisfy(d -> assertThat(d.getEstado()).isEqualTo("EN_DEPOSITO"))
                    .extracting(DonacionResponseDTO::getSubcategoria)
                    .containsExactlyInAnyOrder("arroz", "ropa");
        }

        @Test
        @DisplayName("Persiste exactamente las donaciones que devuelve")
        void persisteLoSegmentado() {
            // El repositorio recibe las mismas donaciones que se devuelven, no otras.
            donanteActivo();

            List<DonacionResponseDTO> creadas = servicio.registrar(pedido("arroz", "ropa"));

            verify(donacionesRepository).cargarDonaciones(donacionesCaptor.capture());
            assertThat(donacionesCaptor.getValue()).extracting(Donacion::getId)
                    .containsExactlyElementsOf(creadas.stream().map(DonacionResponseDTO::getId).toList());
        }

        @Test
        @DisplayName("Donar registra la interacción de la persona donante y la guarda")
        void registraLaInteraccion() {
            // Donar es interactuar con la plataforma: reinicia el plazo de inactividad.
            donanteActivo();

            servicio.registrar(pedido("arroz"));

            verify(donanteRepository).guardar(argThat(persona -> persona.getUltimaInteraccion() != null));
            // El donante ya lo trajo el validador: no se vuelve a buscar.
            verify(donanteRepository, never()).buscarPorId(any());
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
            donanteActivo();

            servicio.registrar(pedido("arroz", "ropa"));

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
            donanteActivo();

            servicio.registrar(pedido("arroz", "arroz", "ropa"));

            // 3 bienes en total, pero solo 2 categorías distintas.
            verify(eventos).publishEvent(eventoCaptor.capture());
            assertThat(eventoCaptor.getValue().cantidadBienes()).isEqualTo(3);
            assertThat(eventoCaptor.getValue().categorias()).containsExactlyInAnyOrder("arroz", "ropa");
        }

        @Test
        @DisplayName("La misma subcategoría escrita distinto cuenta una sola vez, como se escribió primero")
        void categoriasComparadasNormalizadas() {
            // Incentivos cuenta categorías distintas para sus misiones: "Arroz",
            // " arroz " y "ARROZ" no pueden contar como tres.
            donanteActivo();

            servicio.registrar(pedido("Arroz", " arroz ", "ARROZ", "Ropa"));

            verify(eventos).publishEvent(eventoCaptor.capture());
            assertThat(eventoCaptor.getValue().categorias()).containsExactly("Arroz", "Ropa");
        }
    }

    @Nested
    @DisplayName("Persona donante que no puede donar")
    class DonanteQueNoPuedeDonar {

        @Test
        @DisplayName("Una persona donante BLOQUEADA no puede registrar donaciones")
        void donanteBloqueado() {
            when(personasValidator.validarYObtenerPersona(idDonante)).thenReturn(donante("BLOQUEADO"));

            assertThatThrownBy(() -> servicio.registrar(pedido("arroz")))
                    .isInstanceOf(DonanteBloqueadoException.class);
            verify(donacionesRepository, never()).cargarDonaciones(any());
            verifyNoInteractions(eventos);
        }

        @Test
        @DisplayName("Si la persona donante no existe es un 404 y no se registra nada")
        void donanteInexistente() {
            when(personasValidator.validarYObtenerPersona(idDonante))
                    .thenThrow(new PersonaDonanteNoEncontradaException(idDonante));

            assertThatThrownBy(() -> servicio.registrar(pedido("arroz")))
                    .isInstanceOf(PersonaDonanteNoEncontradaException.class);
            verifyNoInteractions(donacionesRepository, eventos);
        }
    }
}
