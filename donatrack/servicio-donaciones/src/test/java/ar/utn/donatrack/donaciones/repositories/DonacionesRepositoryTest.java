package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.interfaces.repositories.FiltroDonaciones;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienGenerico;
import ar.utn.donatrack.donaciones.repositories.jpa.DonacionJpaRepository;
import ar.utn.donatrack.donaciones.util.Pagina;
import ar.utn.donatrack.donaciones.util.SolicitudPagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistencia real de donaciones (Entrega 5): filtros y paginación resueltos
 * en la base, contra H2 en modo PostgreSQL.
 *
 * Estos casos antes se probaban en DonacionServiceTest filtrando una lista en
 * memoria; ahora el filtro es una consulta y se prueba donde ocurre.
 */
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:persistencia_donaciones;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DonacionesRepository.class, RecepcionesEntidadRepository.class})
@DisplayName("DonacionesRepository - persistencia real de donaciones")
class DonacionesRepositoryTest {

    @Autowired private DonacionesRepository repositorio;
    @Autowired private DonacionJpaRepository jpa;
    @Autowired private TestEntityManager em;
    @Autowired private RecepcionesEntidadRepository recepciones;

    private final UUID ana = UUID.randomUUID();
    private final UUID beto = UUID.randomUUID();
    private final UUID comedor = UUID.randomUUID();
    private final SolicitudPagina primeraPagina = new SolicitudPagina(0, 20);

    private Donacion donacion(UUID idDonante, String subcategoria, LocalDateTime fecha) {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(idDonante);
        donacion.setSubcategoria(new Subcategoria(subcategoria));
        donacion.setFechaDonacion(fecha);
        donacion.getBienes().add(BienGenerico.builder()
                .subcategoria(new Subcategoria(subcategoria))
                .descripcion(subcategoria)
                .cantidad(1)
                .unidad("unidades")
                .build());
        return donacion;
    }

    private void persistirYLimpiar(List<Donacion> donaciones) {
        repositorio.cargarDonaciones(donaciones);
        jpa.flush();
        em.clear();
    }

    @Nested
    @DisplayName("Filtros")
    class Filtros {

        private final LocalDateTime base = LocalDateTime.of(2026, 10, 1, 10, 0);
        private Donacion arrozDeAna;
        private Donacion ropaDeAna;
        private Donacion arrozDeBetoAsignado;

        @BeforeEach
        void cargar() {
            arrozDeAna = donacion(ana, "Arroz", base);
            ropaDeAna = donacion(ana, "Ropa", base.plusHours(1));
            arrozDeBetoAsignado = donacion(beto, "Arroz", base.plusHours(2));
            arrozDeBetoAsignado.setIdEntidadBeneficiaria(comedor);
            arrozDeBetoAsignado.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
            persistirYLimpiar(List.of(arrozDeAna, ropaDeAna, arrozDeBetoAsignado));
        }

        @Test
        @DisplayName("vencidas=true devuelve solo las que tienen algún perecedero ya vencido")
        void soloConBienesVencidos() {
            Donacion vencida = donacion(ana, "Leche", base.plusHours(3));
            vencida.getBienes().clear();
            vencida.getBienes().add(BienPerecible.builder().subcategoria(new Subcategoria("Leche"))
                    .descripcion("leche").cantidad(1).unidad("litros")
                    .fechaVencimiento(java.time.LocalDate.of(2020, 1, 1)).build());
            Donacion vigente = donacion(ana, "Yogur", base.plusHours(4));
            vigente.getBienes().clear();
            vigente.getBienes().add(BienPerecible.builder().subcategoria(new Subcategoria("Yogur"))
                    .descripcion("yogur").cantidad(1).unidad("litros")
                    .fechaVencimiento(java.time.LocalDate.of(2099, 1, 1)).build());
            persistirYLimpiar(List.of(vencida, vigente));

            Pagina<Donacion> pagina = repositorio.buscar(
                    new FiltroDonaciones(null, null, null, null, true), primeraPagina);

            assertThat(ids(pagina)).containsExactly(vencida.getId());
        }

        private List<UUID> ids(Pagina<Donacion> pagina) {
            return pagina.contenido().stream().map(Donacion::getId).toList();
        }

        @Test
        @DisplayName("Sin filtros devuelve todas, de la más nueva a la más vieja")
        void sinFiltros() {
            Pagina<Donacion> pagina = repositorio.buscar(FiltroDonaciones.sinFiltros(), primeraPagina);

            assertThat(ids(pagina)).containsExactly(
                    arrozDeBetoAsignado.getId(), ropaDeAna.getId(), arrozDeAna.getId());
            assertThat(pagina.totalElementos()).isEqualTo(3);
        }

        @Test
        @DisplayName("Filtra por estado")
        void porEstado() {
            Pagina<Donacion> pagina = repositorio.buscar(
                    new FiltroDonaciones("EN_DEPOSITO", null, null, null), primeraPagina);

            assertThat(ids(pagina)).containsExactlyInAnyOrder(arrozDeAna.getId(), ropaDeAna.getId());
        }

        @Test
        @DisplayName("Filtra por persona donante")
        void porDonante() {
            Pagina<Donacion> pagina = repositorio.buscar(new FiltroDonaciones(null, beto, null, null), primeraPagina);

            assertThat(ids(pagina)).containsExactly(arrozDeBetoAsignado.getId());
        }

        @Test
        @DisplayName("Filtra por subcategoría sin distinguir mayúsculas ni espacios")
        void porSubcategoria() {
            Pagina<Donacion> pagina = repositorio.buscar(
                    new FiltroDonaciones(null, null, "  ARROZ ", null), primeraPagina);

            assertThat(ids(pagina)).containsExactlyInAnyOrder(arrozDeAna.getId(), arrozDeBetoAsignado.getId());
        }

        @Test
        @DisplayName("Filtra por entidad beneficiaria asignada")
        void porEntidad() {
            Pagina<Donacion> pagina = repositorio.buscar(new FiltroDonaciones(null, null, null, comedor), primeraPagina);

            assertThat(ids(pagina)).containsExactly(arrozDeBetoAsignado.getId());
        }

        @Test
        @DisplayName("Los filtros se combinan: la donación tiene que cumplir todos")
        void combinados() {
            Pagina<Donacion> pagina = repositorio.buscar(
                    new FiltroDonaciones("EN_DEPOSITO", ana, "arroz", null), primeraPagina);

            assertThat(ids(pagina)).containsExactly(arrozDeAna.getId());
        }

        @Test
        @DisplayName("Un filtro en blanco se ignora, igual que si no se hubiera enviado")
        void enBlancoSeIgnora() {
            Pagina<Donacion> pagina = repositorio.buscar(new FiltroDonaciones("  ", null, "  ", null), primeraPagina);

            assertThat(pagina.totalElementos()).isEqualTo(3);
        }

        @Test
        @DisplayName("Las donaciones leídas traen sus bienes y su estado")
        void traeBienesYEstado() {
            Donacion leida = repositorio.buscar(new FiltroDonaciones(null, beto, null, null), primeraPagina)
                    .contenido().getFirst();

            assertThat(leida.getBienes()).hasSize(1);
            assertThat(leida.getEstado().nombre()).isEqualTo("ASIGNACION_REALIZADA");
            assertThat(leida.getHistorialEstados()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Paginación")
    class Paginacion {

        @Test
        @DisplayName("Parte el resultado en páginas estables y calcula los totales")
        void partePaginas() {
            LocalDateTime base = LocalDateTime.of(2026, 10, 1, 10, 0);
            List<Donacion> cinco = List.of(
                    donacion(ana, "Arroz", base), donacion(ana, "Arroz", base.plusMinutes(1)),
                    donacion(ana, "Arroz", base.plusMinutes(2)), donacion(ana, "Arroz", base.plusMinutes(3)),
                    donacion(ana, "Arroz", base.plusMinutes(4)));
            persistirYLimpiar(cinco);

            Pagina<Donacion> primera = repositorio.buscar(FiltroDonaciones.sinFiltros(), new SolicitudPagina(0, 2));
            Pagina<Donacion> tercera = repositorio.buscar(FiltroDonaciones.sinFiltros(), new SolicitudPagina(2, 2));

            assertThat(primera.contenido()).extracting(Donacion::getId)
                    .containsExactly(cinco.get(4).getId(), cinco.get(3).getId());
            assertThat(tercera.contenido()).extracting(Donacion::getId).containsExactly(cinco.get(0).getId());
            assertThat(primera.totalElementos()).isEqualTo(5);
            assertThat(primera.totalPaginas()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Contador desnormalizado de recepciones por entidad")
    class ContadorDeRecepciones {

        private final LocalDate hoy = LocalDate.of(2026, 10, 26);
        private final UUID otraEntidad = UUID.randomUUID();

        private Donacion guardadaEnDeposito() {
            Donacion donacion = donacion(ana, "Arroz", LocalDateTime.of(2026, 10, 1, 10, 0));
            persistirYLimpiar(List.of(donacion));
            return donacion;
        }

        /**
         * Cada asignación parte de la donación leída de la base, como en un pedido
         * real: guardar dos veces el mismo objeto desprendido ahora falla por el
         * bloqueo optimista (su versión quedó vieja después del primer guardado).
         */
        private void asignar(Donacion donacion, UUID idEntidad, LocalDate fecha) {
            Donacion actual = repositorio.buscarPorId(donacion.getId()).orElseThrow();
            em.clear(); // como en un pedido real de hoy: la donación llega desprendida
            actual.setIdEntidadBeneficiaria(idEntidad);
            actual.setFechaAsignacion(fecha);
            repositorio.guardar(actual);
            jpa.flush();
            em.clear();
        }

        private long recibidas(UUID idEntidad) {
            return recepciones.contarRecepcionesDesde(idEntidad, hoy.minusMonths(3));
        }

        @Test
        @DisplayName("Asignar suma una recepción a la entidad en el día de la asignación")
        void asignarSuma() {
            asignar(guardadaEnDeposito(), comedor, hoy);
            asignar(guardadaEnDeposito(), comedor, hoy);

            assertThat(recibidas(comedor)).isEqualTo(2);
            assertThat(recepciones.contarRecepcionesPorEntidadDesde(hoy.minusMonths(3))).containsEntry(comedor, 2L);
        }

        @Test
        @DisplayName("Reasignar mueve la recepción de la entidad anterior a la nueva")
        void reasignarMueve() {
            Donacion donacion = guardadaEnDeposito();
            asignar(donacion, comedor, hoy.minusDays(1));

            asignar(donacion, otraEntidad, hoy);

            assertThat(recibidas(comedor)).isZero();
            assertThat(recibidas(otraEntidad)).isEqualTo(1);
        }

        @Test
        @DisplayName("Guardar una copia vieja de la donación falla en lugar de pisar el cambio de otro (bloqueo optimista)")
        void copiaViejaFalla() {
            Donacion donacion = guardadaEnDeposito();
            Donacion copiaA = repositorio.buscarPorId(donacion.getId()).orElseThrow();
            em.clear();
            Donacion copiaB = repositorio.buscarPorId(donacion.getId()).orElseThrow();
            em.clear();

            copiaA.setDescripcion("cambio de A");
            repositorio.guardar(copiaA);
            jpa.flush();
            em.clear();

            copiaB.setDescripcion("cambio de B");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
                repositorio.guardar(copiaB);
                jpa.flush();
            }).isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        }

        @Test
        @DisplayName("Volver a guardar la misma asignación no la cuenta dos veces")
        void mismaAsignacionNoDuplica() {
            Donacion donacion = guardadaEnDeposito();
            asignar(donacion, comedor, hoy);

            asignar(donacion, comedor, hoy);

            assertThat(recibidas(comedor)).isEqualTo(1);
        }

        @Test
        @DisplayName("Borrar una donación asignada descuenta su recepción")
        void eliminarDescuenta() {
            Donacion donacion = guardadaEnDeposito();
            asignar(donacion, comedor, hoy);

            Donacion actual = repositorio.buscarPorId(donacion.getId()).orElseThrow();
            actual.darDeBaja();
            repositorio.guardar(actual);
            jpa.flush();
            em.clear();

            assertThat(recibidas(comedor)).isZero();
        }

        @Test
        @DisplayName("Una donación dada de baja no se borra pero desaparece de las consultas y de la reconciliación")
        void bajaLogica() {
            Donacion donacion = guardadaEnDeposito();
            asignar(donacion, comedor, hoy);
            Donacion actual = repositorio.buscarPorId(donacion.getId()).orElseThrow();
            actual.cambiarEstado("ASIGNACION_REALIZADA", "asignar", null);
            actual.darDeBaja();
            repositorio.guardar(actual);
            jpa.flush();
            em.clear();

            assertThat(repositorio.buscarPorId(donacion.getId())).isEmpty();
            assertThat(repositorio.obtenerPorEstado("ASIGNACION_REALIZADA")).extracting(Donacion::getId)
                    .doesNotContain(donacion.getId());
            assertThat(repositorio.existenAsignadasA(comedor)).isFalse();
            // La fila y su historial siguen en la base.
            Number filas = (Number) em.getEntityManager().createNativeQuery(
                    "SELECT COUNT(*) FROM donacion WHERE id = :id AND fecha_baja IS NOT NULL")
                    .setParameter("id", donacion.getId()).getSingleResult();
            assertThat(filas.longValue()).isEqualTo(1);
            Number historial = (Number) em.getEntityManager().createNativeQuery(
                    "SELECT COUNT(*) FROM cambio_estado WHERE donacion_id = :id")
                    .setParameter("id", donacion.getId()).getSingleResult();
            assertThat(historial.longValue()).isEqualTo(1);
            assertThat(recibidas(comedor)).isZero();
            // La reconciliación nocturna (SQL nativo) tampoco la cuenta.
            recepciones.reconciliar();
            assertThat(recibidas(comedor)).isZero();
        }

        @Test
        @DisplayName("Con la donación gestionada (sin desprender) asignar y reasignar también mueven el contador")
        void conLaDonacionGestionada() {
            // Es el caso de los services, que trabajan dentro de su transacción. El
            // contador no depende de leer la asignación previa de la base: con la
            // entidad gestionada, esa lectura vería el cambio ya volcado.
            UUID id = guardadaEnDeposito().getId();

            Donacion gestionada = repositorio.buscarPorId(id).orElseThrow();
            gestionada.setIdEntidadBeneficiaria(comedor);
            gestionada.setFechaAsignacion(hoy);
            repositorio.guardar(gestionada);
            jpa.flush();
            assertThat(recibidas(comedor)).isEqualTo(1);

            gestionada.setIdEntidadBeneficiaria(otraEntidad);
            repositorio.guardar(gestionada);
            repositorio.guardar(gestionada); // guardar dos veces no cuenta dos veces
            jpa.flush();
            em.clear();

            assertThat(recibidas(comedor)).isZero();
            assertThat(recibidas(otraEntidad)).isEqualTo(1);
        }

        @Test
        @DisplayName("Guardar sin cambiar la asignación no toca el contador")
        void sinCambioDeAsignacion() {
            UUID id = guardadaEnDeposito().getId();
            Donacion donacion = repositorio.buscarPorId(id).orElseThrow();
            em.clear();

            donacion.setDescripcion("otra descripción");
            repositorio.guardar(donacion);
            jpa.flush();

            assertThat(recepciones.contarRecepcionesPorEntidadDesde(hoy.minusYears(1))).isEmpty();
        }

        @Test
        @DisplayName("Solo cuentan las recepciones dentro de la ventana pedida")
        void respetaLaVentana() {
            asignar(guardadaEnDeposito(), comedor, hoy.minusMonths(10));
            asignar(guardadaEnDeposito(), comedor, hoy.minusDays(10));

            assertThat(recibidas(comedor)).isEqualTo(1);
        }

        @Test
        @DisplayName("La reconciliación recalcula el contador desde la tabla donacion")
        void reconciliacionCorrigeDesfases() {
            asignar(guardadaEnDeposito(), comedor, hoy);
            asignar(guardadaEnDeposito(), comedor, hoy);
            guardadaEnDeposito(); // sin entidad ni fecha: no cuenta
            // Se simula un desfase (por ejemplo, un cambio hecho a mano en la base).
            em.getEntityManager().createNativeQuery("UPDATE entidad_recepcion_diaria SET cantidad = 99")
                    .executeUpdate();
            assertThat(recibidas(comedor)).isEqualTo(99);

            int filas = recepciones.reconciliar();

            assertThat(filas).isEqualTo(1);
            assertThat(recibidas(comedor)).isEqualTo(2);
        }
    }
}
