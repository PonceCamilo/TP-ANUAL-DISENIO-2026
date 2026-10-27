package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionReservadaException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionConBienesVencidosException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionConVariosBienesException;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.interfaces.repositories.FiltroDonaciones;
import ar.utn.donatrack.donaciones.dtos.response.PaginaResponseDTO;
import ar.utn.donatrack.donaciones.clientes.IncentivosClient;
import ar.utn.donatrack.donaciones.clientes.NotificacionClient;
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
import ar.utn.donatrack.donaciones.interfaces.repositories.CandidatosAsignacionRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.asignacion.RankingPrecalculado;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.repositories.PersonaDonanteRepositoryInterface;
import ar.utn.donatrack.donaciones.mappers.DonacionMapper;
import ar.utn.donatrack.donaciones.mappers.EntidadBeneficiariaMapper;
import ar.utn.donatrack.donaciones.mappers.PersonaDonanteMapper;
import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donante.PersonaDonante;
import ar.utn.donatrack.donaciones.models.donante.PersonaHumana;
import ar.utn.donatrack.donaciones.models.donante.estado.ActivoState;
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
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests del servicio principal de donaciones.
 *
 * Cubre las operaciones que expone la API sobre una donación ya segmentada:
 * listarlas con filtros, consultarlas, cambiarles el estado, pedir candidatas
 * para asignación, confirmar la asignación, modificar su bien y eliminarlas.
 *
 * Puntos de integración que se verifican explícitamente:
 *   - Al ASIGNAR se notifica tanto a la entidad como al donante.
 *   - Al llegar a ENTREGADA se avisa a incentivos (métricas del donante).
 *   - Las notificaciones que fallan por falta de email NO frenan la operación.
 *
 * Se usan los mappers y validadores reales (objetos puros) y se mockean los
 * repositorios y los clientes HTTP hacia los otros microservicios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DonacionService - operaciones sobre donaciones")
class DonacionServiceTest {

    @Mock
    private DonacionesRepositoryInterface repositorio;

    @Mock
    private EntidadesBeneficiariasRepositoryInterface entidadesRepositorio;

    @Mock
    private PersonaDonanteRepositoryInterface donanteRepositorio;

    @Mock
    private AsignacionDonacionesService asignacionService;

    @Mock
    private NotificacionClient notificacionClient;

    @Mock
    private IncentivosClient incentivosClient;

    @Mock
    private CandidatosAsignacionRepositoryInterface candidatosRepositorio;

    private DonacionService servicio;

    private Donacion donacion;
    private final UUID idDonante = UUID.randomUUID();

    @BeforeEach
    void prepararEscenario() {
        PersonaDonanteMapper personaMapper = new PersonaDonanteMapper();
        DonacionMapper donacionMapper = new DonacionMapper();
        EntidadBeneficiariaMapper entidadMapper = new EntidadBeneficiariaMapper(personaMapper);

        servicio = new DonacionService(
                repositorio,
                donacionMapper,
                asignacionService,
                entidadesRepositorio,
                entidadMapper,
                donanteRepositorio,
                notificacionClient,
                incentivosClient,
                new DonacionesValidator(repositorio),
                new EntidadesBeneficiariasValidator(entidadesRepositorio),
                candidatosRepositorio);

        donacion = donacionDe("arroz", idDonante);
    }

    /** Donación EN_DEPOSITO con un perecedero que ya venció. */
    private Donacion donacionVencida() {
        Donacion vencida = new Donacion();
        vencida.setIdDonante(idDonante);
        vencida.setSubcategoria(new Subcategoria("leche"));
        vencida.getBienes().add(BienPerecible.builder().subcategoria(new Subcategoria("leche"))
                .descripcion("Leche").cantidad(2).unidad("litros")
                .fechaVencimiento(LocalDate.of(2020, 1, 1)).build());
        return vencida;
    }

    @Test
    @DisplayName("Una donación con bienes vencidos no se asigna ni se le buscan candidatas")
    void donacionVencidaNoSeAsigna() {
        Donacion vencida = donacionVencida();
        when(repositorio.obtenerPorId(vencida.getId())).thenReturn(vencida);
        AsignacionRequestDTO pedido = new AsignacionRequestDTO();
        pedido.setIdEntidadBeneficiaria(UUID.randomUUID());

        assertThatThrownBy(() -> servicio.asignar(vencida.getId(), pedido))
                .isInstanceOf(DonacionConBienesVencidosException.class);
        assertThatThrownBy(() -> servicio.obtenerCandidatos(vencida.getId()))
                .isInstanceOf(DonacionConBienesVencidosException.class);
        verify(repositorio, never()).guardarAsignacion(any());
        assertThat(vencida.estaEnEstado("EN_DEPOSITO")).isTrue();
    }

    /** Donación EN_DEPOSITO con un bien genérico de la subcategoría indicada. */
    private Donacion donacionDe(String subcategoria, UUID donante) {
        Donacion nueva = new Donacion();
        nueva.setIdDonante(donante);
        nueva.setDescripcion("Donación de " + subcategoria);
        nueva.setSubcategoria(new Subcategoria(subcategoria));
        nueva.setBienes(new ArrayList<>(List.of(BienGenerico.builder()
                .subcategoria(new Subcategoria(subcategoria))
                .descripcion("Bien original")
                .cantidad(5)
                .unidad("unidades")
                .build())));
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
                .estado(new ActivoState())
                .build();
    }

    private CambioEstadoRequestDTO dtoCambioEstado(String estado, String justificacion) {
        CambioEstadoRequestDTO dto = new CambioEstadoRequestDTO();
        dto.setEstado(estado);
        dto.setNombreTransicion("transicion");
        dto.setJustificacion(justificacion);
        return dto;
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
            FiltroDonaciones filtro = new FiltroDonaciones("EN_DEPOSITO", idDonante, "arroz", null);
            SolicitudPagina pagina = new SolicitudPagina(1, 5);
            when(repositorio.buscar(filtro, pagina)).thenReturn(new Pagina<>(List.of(donacion), 1, 5, 6));

            PaginaResponseDTO<DonacionResponseDTO> resultado = servicio.obtenerDonaciones(filtro, pagina);

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
            when(repositorio.obtenerPorId(id)).thenReturn(donacion);

            DonacionResponseDTO dto = servicio.obtenerPorId(id);

            assertThat(dto.getId()).isEqualTo(id);
            assertThat(dto.getEstado()).isEqualTo("EN_DEPOSITO");
            assertThat(dto.getSubcategoria()).isEqualTo("arroz");
        }

        @Test
        @DisplayName("obtenerPorId() lanza 404 si la donación no existe")
        void obtenerPorIdInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.obtenerPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.obtenerPorId(idInexistente))
                    .isInstanceOf(DonacionNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Cambio de estado")
    class CambioDeEstado {

        @Test
        @DisplayName("Un cambio válido actualiza el estado y queda registrado en el historial")
        void cambioValido() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("VENCIDA", null));

            assertThat(donacion.estaEnEstado("VENCIDA")).isTrue();
            assertThat(donacion.getHistorialEstados()).hasSize(1);
        }

        @Test
        @DisplayName("No se puede pasar a ASIGNACION_REALIZADA a mano: eso se hace asignando (422)")
        void asignacionManualRechazada() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            assertThatThrownBy(() -> servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("ASIGNACION_REALIZADA", null)))
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
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("VENCIDA", null));

            verify(repositorio).guardar(donacion);
        }

        @Test
        @DisplayName("Un cambio ilegal NO persiste nada")
        void cambioIlegalNoPersiste() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            assertThatThrownBy(() -> servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null)))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);

            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Un cambio ilegal se propaga como excepción de dominio (422)")
        void cambioIlegal() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            assertThatThrownBy(() -> servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null)))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);
        }

        @Test
        @DisplayName("Al pasar a ENTREGADA se notifica a incentivos con el email del donante")
        void entregadaNotificaAIncentivos() {
            // Incentivos usa este evento para actualizar "donaciones exitosas" y
            // "organizaciones ayudadas", que alimentan las insignias del donante.
            avanzarHastaEnTraslado(donacion);
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(donanteConEmail("juan@example.com"));

            servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null));

            verify(incentivosClient).notificarDonacionExitosa(donacion, "juan@example.com", "EMAIL");
        }

        @Test
        @DisplayName("Un cambio a un estado que NO es ENTREGADA no dispara la notificación a incentivos")
        void otrosEstadosNoNotificanAIncentivos() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("VENCIDA", null));

            verifyNoInteractions(incentivosClient);
        }

        @Test
        @DisplayName("Si el donante no tiene email, la entrega se registra igual pero no se notifica")
        void entregadaSinEmailNoNotifica() {
            // La donación ya llegó a destino: el estado debe quedar guardado aunque
            // no se pueda avisar a incentivos.
            avanzarHastaEnTraslado(donacion);
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(donanteConEmail(null));

            servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null));

            assertThat(donacion.fueEntregada()).isTrue();
            verifyNoInteractions(incentivosClient);
        }

        @Test
        @DisplayName("Si el donante fue eliminado, la entrega se registra igual sin romper")
        void entregadaSinDonanteNoRompe() {
            avanzarHastaEnTraslado(donacion);
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(null);

            servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null));

            assertThat(donacion.fueEntregada()).isTrue();
        }

        @Test
        @DisplayName("Si la donación deja EN_DEPOSITO, sale de la bandeja de pendientes")
        void salirDelDepositoLimpiaElRanking() {
            // Una donación vencida no se puede asignar: su ranking no debe seguir
            // en la bandeja hasta el matchmaking de la noche siguiente.
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("VENCIDA", null));

            verify(candidatosRepositorio).eliminar(donacion.getId());
        }

        @Test
        @DisplayName("Un cambio entre estados fuera del depósito no toca la bandeja")
        void cambioFueraDelDepositoNoTocaLaBandeja() {
            donacion.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("LISTA_PARA_ENTREGAR", null));

            verifyNoInteractions(candidatosRepositorio);
        }

        @Test
        @DisplayName("Un cambio ilegal no saca la donación de la bandeja")
        void cambioIlegalNoTocaLaBandeja() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            assertThatThrownBy(() -> servicio.cambiarEstado(donacion.getId(), dtoCambioEstado("ENTREGADA", null)))
                    .isInstanceOf(TransicionDeEstadoIlegalException.class);

            verifyNoInteractions(candidatosRepositorio);
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
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(donanteConEmail("juan@example.com"));

            servicio.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            assertThat(donacion.getIdEntidadBeneficiaria()).isEqualTo(entidad.getId());
            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("Asignar notifica a la ENTIDAD y al DONANTE por separado")
        void notificaAAmbasPartes() {
            // Requisito del enunciado: las dos partes deben enterarse de la asignación.
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(donanteConEmail("juan@example.com"));

            servicio.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            verify(notificacionClient).enviarNotificacion(eq("comedor@lospibes.org"), anyString(), eq("EMAIL"));
            verify(notificacionClient).enviarNotificacion(
                    eq("juan@example.com"), contains("Comedor Los Pibes"), eq("EMAIL"));
        }

        @Test
        @DisplayName("Si la entidad no tiene email, igual se notifica al donante")
        void entidadSinEmailNoFrenaAlDonante() {
            EntidadBeneficiaria entidad = entidadConEmail(null);
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(donanteConEmail("juan@example.com"));

            servicio.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            verify(notificacionClient).enviarNotificacion(eq("juan@example.com"), anyString(), anyString());
            assertThat(donacion.estaEnEstado("ASIGNACION_REALIZADA")).isTrue();
        }

        @Test
        @DisplayName("Si el donante no tiene email, igual se notifica a la entidad")
        void donanteSinEmailNoFrenaALaEntidad() {
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(donanteConEmail(null));

            servicio.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            verify(notificacionClient).enviarNotificacion(eq("comedor@lospibes.org"), anyString(), anyString());
        }

        @Test
        @DisplayName("Lanza 404 si la donación no existe, sin tocar la entidad")
        void donacionInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.obtenerPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.asignar(idInexistente, dtoAsignacion(UUID.randomUUID())))
                    .isInstanceOf(DonacionNoEncontradaException.class);
            verifyNoInteractions(notificacionClient);
        }

        @Test
        @DisplayName("PERSISTE la asignación")
        void persisteLaAsignacion() {
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(donanteConEmail("juan@example.com"));

            servicio.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            // guardarAsignacion y no guardar: así el contador de recepciones de
            // la entidad se actualiza en la misma transacción (Entrega 5).
            verify(repositorio).guardarAsignacion(donacion);
            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Asignar saca la donación de la bandeja de pendientes del administrador")
        void asignarLimpiaElRankingPrecalculado() {
            // Una vez confirmado el destino, el ranking que calculó el proceso
            // nocturno ya no sirve: la donación no vuelve a estar en depósito.
            EntidadBeneficiaria entidad = entidadConEmail("comedor@lospibes.org");
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(entidadesRepositorio.obtenerPorId(entidad.getId())).thenReturn(entidad);
            when(donanteRepositorio.obtenerPersona(idDonante)).thenReturn(donanteConEmail("juan@example.com"));

            servicio.asignar(donacion.getId(), dtoAsignacion(entidad.getId()));

            verify(candidatosRepositorio).eliminar(donacion.getId());
        }

        @Test
        @DisplayName("Lanza 404 si la entidad destino no existe, y la donación queda sin asignar")
        void entidadInexistente() {
            // La validación corre ANTES de tocar la donación: si la entidad no
            // existe, la donación no puede quedar a medio asignar.
            UUID idEntidadInexistente = UUID.randomUUID();
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(entidadesRepositorio.obtenerPorId(idEntidadInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.asignar(donacion.getId(), dtoAsignacion(idEntidadInexistente)))
                    .isInstanceOf(EntidadBeneficiariaNoEncontradaException.class);
            assertThat(donacion.getIdEntidadBeneficiaria()).isNull();
            assertThat(donacion.estaEnEstado("EN_DEPOSITO")).isTrue();
        }
    }

    @Nested
    @DisplayName("Candidatas para asignación")
    class Candidatas {

        @Test
        @DisplayName("Devuelve las tres listas del matchmaking mapeadas a entidades")
        void devuelveLasTresListas() {
            EntidadBeneficiaria entidadA = entidadConEmail("a@example.org");
            EntidadBeneficiaria entidadB = entidadConEmail("b@example.org");

            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(asignacionService.generarRanking(donacion)).thenReturn(
                    new AsignacionDonacionesService.ResultadoMatchmaking(
                            List.of(new ResultadoAsignacion(entidadA.getId(), 3.0)),
                            List.of(new ResultadoAsignacion(entidadA.getId(), 3.0)),
                            List.of(new ResultadoAsignacion(entidadA.getId(), 1.0),
                                    new ResultadoAsignacion(entidadB.getId(), 0.5))));
            when(entidadesRepositorio.obtenerPorIds(anyCollection())).thenReturn(List.of(entidadA, entidadB));

            CandidatosAsignacionResponseDTO respuesta = servicio.obtenerCandidatos(donacion.getId());

            assertThat(respuesta.getIdDonacion()).isEqualTo(donacion.getId());
            assertThat(respuesta.getPorCompatibilidad()).hasSize(1);
            assertThat(respuesta.getPorSubatendidos()).hasSize(2);
            assertThat(respuesta.getCoincidencias()).hasSize(1);
            // Respeta el orden del ranking: primero A (1.0) y después B (0.5).
            assertThat(respuesta.getPorSubatendidos().get(0).getId()).isEqualTo(entidadA.getId());
            assertThat(respuesta.getPorSubatendidos().get(1).getId()).isEqualTo(entidadB.getId());
            // Todas las entidades de las tres listas se cargan en una sola consulta.
            verify(entidadesRepositorio, times(1)).obtenerPorIds(anyCollection());
            verify(entidadesRepositorio, never()).obtenerPorId(any());
        }

        @Test
        @DisplayName("Una entidad del ranking que ya no existe se descarta en lugar de romper la respuesta")
        void entidadBorradaSeDescarta() {
            // Caso borde real: la entidad se eliminó entre que se calculó el
            // ranking y se armó la respuesta.
            UUID idEntidadBorrada = UUID.randomUUID();
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            when(asignacionService.generarRanking(donacion)).thenReturn(
                    new AsignacionDonacionesService.ResultadoMatchmaking(
                            List.of(), List.of(new ResultadoAsignacion(idEntidadBorrada, 3.0)), List.of()));
            when(entidadesRepositorio.obtenerPorIds(anyCollection())).thenReturn(List.of());

            CandidatosAsignacionResponseDTO respuesta = servicio.obtenerCandidatos(donacion.getId());

            assertThat(respuesta.getPorCompatibilidad()).isEmpty();
        }

        @Test
        @DisplayName("Lanza 404 si la donación no existe")
        void donacionInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.obtenerPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.obtenerCandidatos(idInexistente))
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

            when(candidatosRepositorio.obtenerTodos()).thenReturn(List.of(
                    RankingPrecalculado.builder()
                            .idDonacion(donacion.getId())
                            .coincidencias(List.of(new ResultadoAsignacion(entidad.getId(), 3.0)))
                            .rankingSemantico(List.of(new ResultadoAsignacion(entidad.getId(), 3.0)))
                            .rankingSubAtendidos(List.of(new ResultadoAsignacion(entidad.getId(), 1.0)))
                            .fechaCalculo(calculadoA)
                            .build()));
            when(entidadesRepositorio.obtenerPorIds(anyCollection())).thenReturn(List.of(entidad));

            List<CandidatosPendientesResponseDTO> pendientes = servicio.obtenerCandidatosPendientes();

            assertThat(pendientes).hasSize(1);
            assertThat(pendientes.getFirst().getIdDonacion()).isEqualTo(donacion.getId());
            assertThat(pendientes.getFirst().getFechaCalculo()).isEqualTo(calculadoA);
            assertThat(pendientes.getFirst().isHuboCoincidencias()).isTrue();
            assertThat(pendientes.getFirst().getCoincidencias()).hasSize(1);
        }

        @Test
        @DisplayName("Marca huboCoincidencias en false cuando los algoritmos no se pusieron de acuerdo")
        void sinCoincidencias() {
            // Es la señal para el administrador de que tiene que mirar las dos
            // listas y decidir, en lugar de aceptar una recomendación de consenso.
            when(candidatosRepositorio.obtenerTodos()).thenReturn(List.of(
                    RankingPrecalculado.builder()
                            .idDonacion(donacion.getId())
                            .coincidencias(List.of())
                            .rankingSemantico(List.of())
                            .rankingSubAtendidos(List.of())
                            .fechaCalculo(LocalDateTime.now())
                            .build()));

            assertThat(servicio.obtenerCandidatosPendientes().getFirst().isHuboCoincidencias()).isFalse();
        }

        @Test
        @DisplayName("Devuelve lista vacía si el proceso nocturno todavía no corrió")
        void sinCorridaPrevia() {
            when(candidatosRepositorio.obtenerTodos()).thenReturn(List.of());

            assertThat(servicio.obtenerCandidatosPendientes()).isEmpty();
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
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.modificarUnicoBien(donacion.getId(), dtoBien("arroz", null));

            assertThat(donacion.getBienes()).hasSize(1);
            assertThat(donacion.getBienes().getFirst().getDescripcion()).isEqualTo("Bien corregido");
            assertThat(donacion.getBienes().getFirst().getCantidad()).isEqualTo(10);
        }

        @Test
        @DisplayName("Si el bien enviado trae vencimiento, se crea como bien perecedero")
        void bienConVencimientoEsPerecedero() {
            // El tipo concreto del bien lo decide el mapper según los campos
            // presentes en el request; la corrección puede cambiarlo.
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.modificarUnicoBien(donacion.getId(), dtoBien("arroz", LocalDate.of(2027, 1, 1)));

            assertThat(donacion.getBienes().getFirst()).isInstanceOf(BienPerecible.class);
            assertThat(donacion.esPerecible()).isTrue();
        }

        @Test
        @DisplayName("Lanza DonacionSinBienes si la donación no tiene ningún bien que modificar")
        void sinBienesLanza() {
            Donacion sinBienes = new Donacion();
            when(repositorio.obtenerPorId(sinBienes.getId())).thenReturn(sinBienes);

            assertThatThrownBy(() -> servicio.modificarUnicoBien(sinBienes.getId(), dtoBien("arroz", null)))
                    .isInstanceOf(DonacionSinBienesException.class);
        }

        @Test
        @DisplayName("modificarBien() reemplaza el bien indicado por id y devuelve la donación")
        void modificaUnBienPorId() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);
            UUID idBien = donacion.getBienes().getFirst().getId();

            DonacionResponseDTO respuesta = servicio.modificarBien(donacion.getId(), idBien, dtoBien("arroz", null));

            assertThat(respuesta.getBienes()).singleElement()
                    .satisfies(bien -> assertThat(bien.getDescripcion()).isEqualTo("Bien corregido"));
            verify(repositorio).guardar(donacion);
        }

        @Test
        @DisplayName("modificarUnicoBien() con varios bienes pide indicar cuál (409)")
        void variosBienesPideIndicarCual() {
            donacion.getBienes().add(BienGenerico.builder()
                    .subcategoria(new Subcategoria("arroz")).descripcion("Otro").cantidad(1).unidad("kg").build());
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            assertThatThrownBy(() -> servicio.modificarUnicoBien(donacion.getId(), dtoBien("arroz", null)))
                    .isInstanceOf(DonacionConVariosBienesException.class);
        }

        @Test
        @DisplayName("eliminar() valida que la donación exista antes de borrarla")
        void eliminaLaDonacion() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.eliminar(donacion.getId());

            verify(repositorio).eliminar(donacion.getId());
        }

        @Test
        @DisplayName("eliminar() también saca la donación de la bandeja de pendientes")
        void eliminarLimpiaElRanking() {
            when(repositorio.obtenerPorId(donacion.getId())).thenReturn(donacion);

            servicio.eliminar(donacion.getId());

            verify(candidatosRepositorio).eliminar(donacion.getId());
        }

        @Test
        @DisplayName("eliminar() lanza 404 y no borra nada si la donación no existe")
        void eliminarInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.obtenerPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.eliminar(idInexistente))
                    .isInstanceOf(DonacionNoEncontradaException.class);
            verify(repositorio, never()).eliminar(any());
            verifyNoInteractions(candidatosRepositorio);
        }
    }
}
