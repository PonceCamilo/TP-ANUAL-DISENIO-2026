package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.mappers.MappersDePrueba;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionEntregada;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionDadaDeBaja;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionCambioDeEstado;
import ar.utn.donatrack.donaciones.models.donacion.eventos.DonacionAsignada;
import java.util.Map;
import java.util.LinkedHashMap;
import ar.utn.donatrack.donaciones.dtos.response.RankingAlgoritmoResponseDTO;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionReservadaException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionConBienesVencidosException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionConVariosBienesException;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.FiltroDonaciones;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.dtos.request.AsignacionRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.BienRequestDTO;
import ar.utn.donatrack.donaciones.dtos.request.CambioEstadoRequestDTO;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosAsignacionResponseDTO;
import ar.utn.donatrack.donaciones.dtos.response.DonacionResponseDTO;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionDeEstadoIlegalException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionSinBienesException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.dtos.response.CandidatosPendientesResponseDTO;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioCandidatosAsignacion;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioEntidadesBeneficiarias;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.validations.donaciones.DonacionesValidator;
import ar.utn.donatrack.donaciones.validations.entidades.EntidadesBeneficiariasValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del servicio principal de donaciones.
 *
 * Cubre las operaciones que expone la API sobre una donación ya segmentada:
 * listarlas con filtros, consultarlas, cambiarles el estado, pedir candidatas
 * para asignación, confirmar la asignación, modificar su bien y eliminarlas.
 *
 * Lo que depende de cada operación (avisos, incentivos, bandeja, necesidades)
 * lo hacen los oyentes de los eventos de dominio y se prueba en el paquete
 * eventos; acá se verifica que la donación deje el evento correcto.
 *
 * Se usan los mappers y validadores reales (objetos puros) y se mockean los
 * repositorios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DonacionComandosService y DonacionConsultasService - operaciones sobre donaciones")
class DonacionServiciosTest {

    @Mock
    private RepositorioDonaciones repositorio;

    @Mock
    private RepositorioEntidadesBeneficiarias entidadesRepositorio;

    @Mock
    private AsignacionDonacionesService asignacionService;

    @Mock
    private RepositorioCandidatosAsignacion candidatosRepositorio;

    private DonacionComandosService comandos;
    private DonacionConsultasService consultas;

    private Donacion donacion;
    private final UUID idDonante = UUID.randomUUID();

    @BeforeEach
    void prepararEscenario() {
        DonacionMapper donacionMapper = new DonacionMapper();
        EntidadBeneficiariaMapper entidadMapper = MappersDePrueba.entidades();

        comandos = new DonacionComandosService(
                repositorio,
                donacionMapper,
                new DonacionesValidator(repositorio),
                new EntidadesBeneficiariasValidator(entidadesRepositorio));
        consultas = new DonacionConsultasService(
                repositorio,
                donacionMapper,
                asignacionService,
                entidadesRepositorio,
                entidadMapper,
                new DonacionesValidator(repositorio),
                candidatosRepositorio);

        donacion = donacionDe("arroz", idDonante);
    }

    /** Donación EN_DEPOSITO con un perecedero que ya venció. */
    private Donacion donacionVencida() {
        Donacion vencida = new Donacion();
        vencida.setIdDonante(idDonante);
        vencida.setSubcategoria(new Subcategoria("leche"));
        vencida.agregarBien(BienPerecible.builder().subcategoria(new Subcategoria("leche"))
                .descripcion("Leche").cantidad(2).unidad("litros")
                .fechaVencimiento(LocalDate.of(2020, 1, 1)).build());
        return vencida;
    }

    @Test
    @DisplayName("Una donación con bienes vencidos no se asigna ni se le buscan candidatas")
    void donacionVencidaNoSeAsigna() {
        Donacion vencida = donacionVencida();
        when(repositorio.buscarPorId(vencida.getId())).thenReturn(Optional.of(vencida));
        AsignacionRequestDTO pedido = new AsignacionRequestDTO();
        pedido.setIdEntidadBeneficiaria(UUID.randomUUID());

        assertThatThrownBy(() -> comandos.asignar(vencida.getId(), pedido))
                .isInstanceOf(DonacionConBienesVencidosException.class);
        assertThatThrownBy(() -> consultas.obtenerCandidatos(vencida.getId()))
                .isInstanceOf(DonacionConBienesVencidosException.class);
        verify(repositorio, never()).guardar(any());
        assertThat(vencida.estaEnEstado("EN_DEPOSITO")).isTrue();
    }

    /** Donación EN_DEPOSITO con un bien genérico de la subcategoría indicada. */
    private Donacion donacionDe(String subcategoria, UUID donante) {
        Donacion nueva = new Donacion();
        nueva.setIdDonante(donante);
        nueva.setDescripcion("Donación de " + subcategoria);
        nueva.setSubcategoria(new Subcategoria(subcategoria));
        nueva.agregarBien(BienGenerico.builder()
                .subcategoria(new Subcategoria(subcategoria))
                .descripcion("Bien original")
                .cantidad(5)
                .unidad("unidades")
                .build());
        return nueva;
    }

    private EntidadBeneficiaria entidadConEmail(String email) {
        return EntidadBeneficiaria.builder()
                .id(UUID.randomUUID())
                .razonSocial("Comedor Los Pibes")
                .contactos(email == null ? new ArrayList<>() : List.of(Email.builder().valor(email).build()))
                .campanias(new ArrayList<>())
                .build();
    }

    private PersonaDonante donanteConEmail(String email) {
        return PersonaHumana.builder()
                .id(idDonante)
                .nombre("Juan")
                .apellido("Pérez")
                .email(email)
                .build();
    }

    private CambioEstadoRequestDTO dtoCambioEstado(String estado, String justificacion) {
        CambioEstadoRequestDTO dto = new CambioEstadoRequestDTO();
        dto.setEstado(estado);
        dto.setJustificacion(justificacion);
        return dto;
    }

    /** Resultado del matchmaking con el ranking de cada uno de los dos algoritmos. */
    private static ResultadoMatchmaking matchmaking(List<ResultadoAsignacion> semantico,
                                                    List<ResultadoAsignacion> subAtendidos) {
        Map<String, List<ResultadoAsignacion>> rankings = new LinkedHashMap<>();
        rankings.put(AlgoritmoCompatibilidadSemantica.NOMBRE, semantico);
        rankings.put(AlgoritmoPrioridadSubAtendidos.NOMBRE, subAtendidos);
        return new ResultadoMatchmaking(rankings);
    }

    /** Los eventos de dominio que dejó la donación (el repositorio simulado no los publica). */
    private static <E> List<E> eventos(Donacion donacion, Class<E> tipo) {
        return donacion.eventosPendientes().stream().filter(tipo::isInstance).map(tipo::cast).toList();
    }

    /** Avanza la donación hasta EN_TRASLADO, que es el paso previo a ENTREGADA. */
    private void avanzarHastaEnTraslado(Donacion objetivo) {
        objetivo.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
        objetivo.cambiarEstado("LISTA_PARA_ENTREGAR", "planificar", null);
        objetivo.cambiarEstado("EN_TRASLADO", "iniciar ruta", null);
    }

    @Nested
    @DisplayName("Consulta y filtrado de donaciones")
    class ConsultaYFiltrado {

        @Test
        @DisplayName("Delega filtros y página al repositorio y devuelve la página mapeada a DTOs")
        void delegaEnElRepositorio() {
            // Los filtros los resuelve la base (Entrega 5); su semántica se prueba
            // contra H2 en DonacionesRepositoryTest. Acá se verifica el pasamanos.
            FiltroDonaciones filtro = new FiltroDonaciones("EN_DEPOSITO", idDonante, "arroz", null, false);
            SolicitudPagina pagina = new SolicitudPagina(1, 5);
            when(repositorio.buscar(filtro, pagina)).thenReturn(new Pagina<>(List.of(donacion), 1, 5, 6));

            PaginaResponseDTO<DonacionResponseDTO> resultado = consultas.obtenerDonaciones(filtro, pagina);

            assertThat(resultado.getContenido()).hasSize(1);
            assertThat(resultado.getContenido().getFirst().getSubcategoria()).isEqualTo("arroz");
            assertThat(resultado.getPagina()).isEqualTo(1);
            assertThat(resultado.getTamanio()).isEqualTo(5);
            assertThat(resultado.getTotalElementos()).isEqualTo(6);
            assertThat(resultado.getTotalPaginas()).isEqualTo(2);
        }

        @Test
        @DisplayName("obtenerPorId() devuelve la donación con su estado e historial")
        void obtenerPorId() {
            UUID id = donacion.getId();
            when(repositorio.buscarPorId(id)).thenReturn(Optional.of(donacion));

            DonacionResponseDTO dto = consultas.obtenerPorId(id);

            assertThat(dto.getId()).isEqualTo(id);
            assertThat(dto.getEstado()).isEqualTo("EN_DEPOSITO");
            assertThat(dto.getSubcategoria()).isEqualTo("arroz");
        }

        @Test
        @DisplayName("obtenerPorId() lanza 404 si la donación no existe")
        void obtenerPorIdInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultas.obtenerPorId(idInexistente))
                    .isInstanceOf(DonacionNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Cambio de estado")
    class CambioDeEstado {

        @Test
        @DisplayName("Un cambio válido actualiza el estado y queda registrado en el historial")
        void cambioValido() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("VENCIDA", null));

            assertThat(donacion.estaEnEstado("VENCIDA")).isTrue();
            assertThat(donacion.getHistorialEstados()).hasSize(1);
        }

        @Test
        @DisplayName("No se puede pasar a ASIGNACION_REALIZADA a mano: eso se hace asignando (422)")
        void asignacionManualRechazada() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            assertThatThrownBy(() -> comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("ASIGNACION_REALIZADA", null)))
                    .isInstanceOf(TransicionReservadaException.class);

            verify(repositorio, never()).guardar(any());
            assertThat(donacion.getIdEntidadBeneficiaria()).isNull();
        }

        @Test
        @DisplayName("PERSISTE el cambio de estado")
        void persisteElCambioDeEstado() {
            // Sin este guardado el cambio se pierde: con JPA la entidad que
            // devuelve el repositorio queda detached al cerrar la transacción de
            // lectura. Es el bug que los tests con mock NO detectaban y que
            // apareció recién al probar contra PostgreSQL real.
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("VENCIDA", null));

            verify(repositorio).guardar(donacion);
        }

        @Test
        @DisplayName("Un cambio ilegal NO persiste nada")
        void cambioIlegalNoPersiste() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            assertThatThrownBy(() -> comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null)))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);

            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Un cambio ilegal se propaga como excepción de dominio (422)")
        void cambioIlegal() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            assertThatThrownBy(() -> comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null)))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);
        }

        @Test
        @DisplayName("Al pasar a ENTREGADA la donación deja el evento de entrega (necesidades e incentivos)")
        void entregadaDejaElEvento() {
            avanzarHastaEnTraslado(donacion);
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null));

            assertThat(eventos(donacion, DonacionEntregada.class)).singleElement().satisfies(evento -> {
                assertThat(evento.idDonacion()).isEqualTo(donacion.getId());
                assertThat(evento.idDonante()).isEqualTo(idDonante);
                assertThat(evento.cantidad()).isEqualTo(donacion.cantidadTotal());
                assertThat(evento.fechaEntrega()).isNotNull();
            });
            verify(repositorio).guardar(donacion);
        }

        @Test
        @DisplayName("Un cambio a un estado que NO es ENTREGADA no deja evento de entrega")
        void otrosEstadosNoSonEntrega() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("VENCIDA", null));

            assertThat(eventos(donacion, DonacionEntregada.class)).isEmpty();
            assertThat(eventos(donacion, DonacionCambioDeEstado.class)).singleElement()
                    .satisfies(evento -> assertThat(evento.estadoNuevo()).isEqualTo("VENCIDA"));
        }

        @Test
        @DisplayName("Si la donación deja EN_DEPOSITO, el evento lo indica (su ranking sale de la bandeja)")
        void salirDelDepositoLoIndica() {
            // Una donación vencida no se puede asignar: su ranking no debe seguir
            // en la bandeja hasta el matchmaking de la noche siguiente.
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("VENCIDA", null));

            assertThat(eventos(donacion, DonacionCambioDeEstado.class)).singleElement()
                    .satisfies(evento -> assertThat(evento.salioDelDeposito()).isTrue());
        }

        @Test
        @DisplayName("Un cambio entre estados fuera del depósito no indica salida del depósito")
        void cambioFueraDelDepositoNoTocaLaBandeja() {
            donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("LISTA_PARA_ENTREGAR", null));

            assertThat(eventos(donacion, DonacionCambioDeEstado.class)).last()
                    .satisfies(evento -> assertThat(evento.salioDelDeposito()).isFalse());
        }

        @Test
        @DisplayName("Un cambio ilegal no deja eventos ni guarda nada")
        void cambioIlegalNoTocaLaBandeja() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            assertThatThrownBy(() -> comandos.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null)))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);

            assertThat(donacion.eventosPendientes()).isEmpty();
            verify(repositorio, never()).guardar(any());
        }

    }

    @Nested
    @DisplayName("Asignación a una entidad beneficiaria")
    class Asignacion {

        private AsignacionRequestDTO dtoAsignacion(UUID idEntidad) {
            AsignacionRequestDTO dto = new AsignacionRequestDTO();
            dto.setIdEntidadBeneficiaria(idEntidad);
            return dto;
        }

        @Test
        @DisplayName("Asignar guarda la entidad en la donación y la pasa a ASIGNACION_REALIZADA")
        void asignaCorrectamente() {
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));

            comandos.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            assertThat(donacion.getIdEntidadBeneficiaria()).isEqualTo(entidad.getId());
            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("Asignar deja el evento de asignación con la persona donante y la entidad")
        void dejaElEventoDeAsignacion() {
            // Requisito del enunciado: las dos partes deben enterarse de la
            // asignación. El aviso lo manda AvisosOyente después de confirmar.
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));

            comandos.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            assertThat(eventos(donacion, DonacionAsignada.class)).containsExactly(
                    new DonacionAsignada(donacion.getId(), idDonante, entidad.getId()));
        }

        @Test
        @DisplayName("Lanza 404 si la donación no existe, sin tocar la entidad")
        void donacionInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> comandos.asignar(idInexistente, dtoAsignacion(UUID.randomUUID())))
                    .isInstanceOf(DonacionNoEncontradaException.class);
            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("PERSISTE la asignación")
        void persisteLaAsignacion() {
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));

            comandos.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            // Un solo guardado: el repositorio mueve el contador de recepciones
            // porque la asignación de la donación cambió.
            verify(repositorio).guardar(donacion);
            assertThat(donacion.cambioLaAsignacion()).isTrue();
        }

        @Test
        @DisplayName("Asignar indica que la donación dejó el depósito (su ranking sale de la bandeja)")
        void asignarSaleDelDeposito() {
            // Una vez confirmado el destino, el ranking que calculó el proceso
            // nocturno ya no sirve: la donación no vuelve a estar en depósito.
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(entidad.getId())).thenReturn(Optional.of(entidad));

            comandos.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            assertThat(eventos(donacion, DonacionCambioDeEstado.class)).singleElement()
                    .satisfies(evento -> assertThat(evento.salioDelDeposito()).isTrue());
        }

        @Test
        @DisplayName("Lanza 404 si la entidad destino no existe, y la donación queda sin asignar")
        void entidadInexistente() {
            // La validación corre ANTES de tocar la donación: si la entidad no
            // existe, la donación no puede quedar a medio asignar.
            UUID idEntidadInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(entidadesRepositorio.buscarPorId(idEntidadInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> comandos.asignar(donacion.getId(), dtoAsignacion(idEntidadInexistente)))
                    .isInstanceOf(EntidadBeneficiariaNoEncontradaException.class);
            assertThat(donacion.getIdEntidadBeneficiaria()).isNull();
            assertThat(donacion.estaEnEstado("EN_DEPOSITO")).isTrue();
        }
    }

    @Nested
    @DisplayName("Candidatas para asignación")
    class Candidatas {

        @Test
        @DisplayName("Devuelve el ranking de cada algoritmo y las coincidencias, mapeados a entidades")
        void devuelveLosRankings() {
            EntidadBeneficiaria entidadA = entidadConEmail("a@example.org");
            EntidadBeneficiaria entidadB = entidadConEmail("b@example.org");

            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(asignacionService.generarRanking(donacion)).thenReturn(matchmaking(
                    List.of(new ResultadoAsignacion(entidadA.getId(), 3.0)),
                    List.of(new ResultadoAsignacion(entidadA.getId(), 1.0),
                            new ResultadoAsignacion(entidadB.getId(), 0.5))));
            when(entidadesRepositorio.obtenerPorIds(anyCollection())).thenReturn(List.of(entidadA, entidadB));

            CandidatosAsignacionResponseDTO respuesta = consultas.obtenerCandidatos(donacion.getId());

            assertThat(respuesta.getIdDonacion()).isEqualTo(donacion.getId());
            assertThat(respuesta.getRankings()).extracting(RankingAlgoritmoResponseDTO::algoritmo)
                    .containsExactly(AlgoritmoCompatibilidadSemantica.NOMBRE, AlgoritmoPrioridadSubAtendidos.NOMBRE);
            assertThat(respuesta.getRankings().get(0).entidades()).hasSize(1);
            assertThat(respuesta.getRankings().get(1).entidades()).hasSize(2);
            assertThat(respuesta.getCoincidencias()).hasSize(1);
            // Respeta el orden del ranking: primero A (1.0) y después B (0.5).
            assertThat(respuesta.getRankings().get(1).entidades().get(0).getId()).isEqualTo(entidadA.getId());
            assertThat(respuesta.getRankings().get(1).entidades().get(1).getId()).isEqualTo(entidadB.getId());
            // Todas las entidades de las tres listas se cargan en una sola consulta.
            verify(entidadesRepositorio, times(1)).obtenerPorIds(anyCollection());
            verify(entidadesRepositorio, never()).buscarPorId(any());
        }

        @Test
        @DisplayName("Una entidad del ranking que ya no existe se descarta en lugar de romper la respuesta")
        void entidadBorradaSeDescarta() {
            // Caso borde real: la entidad se eliminó entre que se calculó el
            // ranking y se armó la respuesta.
            UUID idEntidadBorrada = UUID.randomUUID();
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            when(asignacionService.generarRanking(donacion)).thenReturn(
                    matchmaking(List.of(new ResultadoAsignacion(idEntidadBorrada, 3.0)), List.of()));
            when(entidadesRepositorio.obtenerPorIds(anyCollection())).thenReturn(List.of());

            CandidatosAsignacionResponseDTO respuesta = consultas.obtenerCandidatos(donacion.getId());

            assertThat(respuesta.getRankings().getFirst().entidades()).isEmpty();
        }

        @Test
        @DisplayName("Lanza 404 si la donación no existe")
        void donacionInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consultas.obtenerCandidatos(idInexistente))
                    .isInstanceOf(DonacionNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Bandeja de pendientes del matchmaking nocturno")
    class CandidatosPendientes {

        @Test
        @DisplayName("Devuelve los rankings que dejó el proceso nocturno, con su fecha de cálculo")
        void devuelveLosRankingsDelBatch() {
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            LocalDateTime calculadoA = LocalDateTime.of(2026, 3, 15, 3, 30);

            when(candidatosRepositorio.obtenerTodos()).thenReturn(List.of(RankingPrecalculado.de(donacion.getId(),
                    matchmaking(List.of(new ResultadoAsignacion(entidad.getId(), 3.0)),
                            List.of(new ResultadoAsignacion(entidad.getId(), 1.0))),
                    calculadoA)));
            when(entidadesRepositorio.obtenerPorIds(anyCollection())).thenReturn(List.of(entidad));

            List<CandidatosPendientesResponseDTO> pendientes = consultas.obtenerCandidatosPendientes();

            assertThat(pendientes).hasSize(1);
            assertThat(pendientes.getFirst().getIdDonacion()).isEqualTo(donacion.getId());
            assertThat(pendientes.getFirst().getFechaCalculo()).isEqualTo(calculadoA);
            assertThat(pendientes.getFirst().isHuboCoincidencias()).isTrue();
            assertThat(pendientes.getFirst().getCoincidencias()).hasSize(1);
            assertThat(pendientes.getFirst().getRankings()).hasSize(2);
        }

        @Test
        @DisplayName("Marca huboCoincidencias en false cuando los algoritmos no se pusieron de acuerdo")
        void sinCoincidencias() {
            // Es la señal para el administrador de que tiene que mirar las dos
            // listas y decidir, en lugar de aceptar una recomendación de consenso.
            when(candidatosRepositorio.obtenerTodos()).thenReturn(List.of(RankingPrecalculado.de(donacion.getId(),
                    matchmaking(List.of(new ResultadoAsignacion(UUID.randomUUID(), 3.0)),
                            List.of(new ResultadoAsignacion(UUID.randomUUID(), 1.0))),
                    LocalDateTime.now())));

            assertThat(consultas.obtenerCandidatosPendientes().getFirst().isHuboCoincidencias()).isFalse();
        }

        @Test
        @DisplayName("Devuelve lista vacía si el proceso nocturno todavía no corrió")
        void sinCorridaPrevia() {
            when(candidatosRepositorio.obtenerTodos()).thenReturn(List.of());

            assertThat(consultas.obtenerCandidatosPendientes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Modificación del bien y baja de la donación")
    class ModificacionYBaja {

        private BienRequestDTO dtoBien(String subcategoria, LocalDate vencimiento) {
            BienRequestDTO dto = new BienRequestDTO();
            dto.setSubcategoria(subcategoria);
            dto.setDescripcion("Bien corregido");
            dto.setCantidad(10);
            dto.setUnidad("kg");
            dto.setFechaVencimiento(vencimiento);
            return dto;
        }

        @Test
        @DisplayName("Reemplaza el bien de la donación por el enviado")
        void modificaElBien() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.modificarUnicoBien(donacion.getId(), dtoBien("arroz", null));

            assertThat(donacion.getBienes()).hasSize(1);
            assertThat(donacion.getBienes().getFirst().getDescripcion()).isEqualTo("Bien corregido");
            assertThat(donacion.getBienes().getFirst().getCantidad()).isEqualTo(10);
        }

        @Test
        @DisplayName("Si el bien enviado trae vencimiento, se crea como bien perecedero")
        void bienConVencimientoEsPerecedero() {
            // El tipo concreto del bien lo decide el mapper según los campos
            // presentes en el request; la corrección puede cambiarlo.
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.modificarUnicoBien(donacion.getId(), dtoBien("arroz", LocalDate.of(2027, 1, 1)));

            assertThat(donacion.getBienes().getFirst()).isInstanceOf(BienPerecible.class);
        }

        @Test
        @DisplayName("Lanza DonacionSinBienes si la donación no tiene ningún bien que modificar")
        void sinBienesLanza() {
            Donacion sinBienes = new Donacion();
            when(repositorio.buscarPorId(sinBienes.getId())).thenReturn(Optional.of(sinBienes));

            assertThatThrownBy(() -> comandos.modificarUnicoBien(sinBienes.getId(), dtoBien("arroz", null)))
                    .isInstanceOf(DonacionSinBienesException.class);
        }

        @Test
        @DisplayName("modificarBien() reemplaza el bien indicado por id y devuelve la donación")
        void modificaUnBienPorId() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));
            UUID idBien = donacion.getBienes().getFirst().getId();

            DonacionResponseDTO respuesta = comandos.modificarBien(donacion.getId(), idBien, dtoBien("arroz", null));

            assertThat(respuesta.getBienes()).singleElement()
                    .satisfies(bien -> assertThat(bien.getDescripcion()).isEqualTo("Bien corregido"));
            verify(repositorio).guardar(donacion);
        }

        @Test
        @DisplayName("modificarUnicoBien() con varios bienes pide indicar cuál (409)")
        void variosBienesPideIndicarCual() {
            donacion.agregarBien(BienGenerico.builder()
                    .subcategoria(new Subcategoria("arroz")).descripcion("Otro").cantidad(1).unidad("kg").build());
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            assertThatThrownBy(() -> comandos.modificarUnicoBien(donacion.getId(), dtoBien("arroz", null)))
                    .isInstanceOf(DonacionConVariosBienesException.class);
        }

        @Test
        @DisplayName("eliminar() valida que la donación exista antes de borrarla")
        void eliminaLaDonacion() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.eliminar(donacion.getId());

            // Baja lógica: se guarda la donación marcada, no se borra.
            assertThat(donacion.estaDadaDeBaja()).isTrue();
            verify(repositorio).guardar(donacion);
        }

        @Test
        @DisplayName("eliminar() deja el evento de baja (su ranking sale de la bandeja)")
        void eliminarDejaElEventoDeBaja() {
            when(repositorio.buscarPorId(donacion.getId())).thenReturn(Optional.of(donacion));

            comandos.eliminar(donacion.getId());

            assertThat(eventos(donacion, DonacionDadaDeBaja.class)).containsExactly(new DonacionDadaDeBaja(donacion.getId()));
        }

        @Test
        @DisplayName("eliminar() lanza 404 y no borra nada si la donación no existe")
        void eliminarInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> comandos.eliminar(idInexistente))
                    .isInstanceOf(DonacionNoEncontradaException.class);
            verify(repositorio, never()).guardar(any());
        }
    }
}
