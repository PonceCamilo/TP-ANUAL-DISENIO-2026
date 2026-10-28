package ar.utn.donatrack.donaciones.repositories;

import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.interfaces.repositories.FiltroDonaciones;
import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
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

import java.util.stream.Stream;
import java.util.ArrayList;
import java.time.Clock;
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

    private static EntidadBeneficiaria entidad(UUID id) {
        return EntidadBeneficiaria.builder().id(id).razonSocial("Entidad " + id).build();
    }

    /**
     * Asigna como lo hace el dominio (asignarA), en la fecha indicada. Si ya estaba
     * asignada, antes pasa por una entrega fallida y vuelve al depósito, que es la
     * forma en que una donación se reasigna.
     */
    private static void asignarEn(Donacion donacion, UUID idEntidad, LocalDate fecha) {
        if (donacion.estaEnEstado("ASIGNACION_REALIZADA")) {
            donacion.cambiarEstado("LISTA_PARA_ENTREGAR", "despachar", null);
            donacion.cambiarEstado("EN_TRASLADO", "inicio de ruta", null);
            donacion.cambiarEstado("ENTREGA_FALLIDA", "entrega fallida", "nadie en el domicilio");
            donacion.cambiarEstado("EN_DEPOSITO", "replanificar", null);
        }
        FechaHoraArgentina.usarReloj(Clock.fixed(fecha.atTime(12, 0).atZone(FechaHoraArgentina.ZONA).toInstant(),
                FechaHoraArgentina.ZONA));
        try {
            donacion.asignarA(entidad(idEntidad));
        } finally {
            FechaHoraArgentina.restablecerReloj();
        }
    }

    private Donacion donacion(UUID idDonante, String subcategoria, LocalDateTime fecha) {
        return donacion(idDonante, subcategoria, fecha, BienGenerico.builder()
                .subcategoria(new Subcategoria(subcategoria))
                .descripcion(subcategoria)
                .cantidad(1)
                .unidad("unidades")
                .build());
    }

    private Donacion donacion(UUID idDonante, String subcategoria, LocalDateTime fecha, Bien bien) {
        Donacion donacion = new Donacion();
        donacion.setIdDonante(idDonante);
        donacion.setSubcategoria(new Subcategoria(subcategoria));
        donacion.setFechaDonacion(fecha);
        donacion.agregarBien(bien);
        return donacion;
    }

    private static Bien perecible(String subcategoria, LocalDate vence) {
        return BienPerecible.builder().subcategoria(new Subcategoria(subcategoria))
                .descripcion(subcategoria).cantidad(1).unidad("litros")
                .fechaVencimiento(vence).build();
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
            arrozDeBetoAsignado.asignarA(entidad(comedor));
            persistirYLimpiar(List.of(arrozDeAna, ropaDeAna, arrozDeBetoAsignado));
        }

        @Test
        @DisplayName("vencidas=true devuelve solo las que tienen algún perecedero ya vencido")
        void soloConBienesVencidos() {
            Donacion vencida = donacion(ana, "Leche", base.plusHours(3), perecible("Leche", LocalDate.of(2020, 1, 1)));
            Donacion vigente = donacion(ana, "Yogur", base.plusHours(4), perecible("Yogur", LocalDate.of(2099, 1, 1)));
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
            Pagina<Donacion> pagina = repositorio.buscar(new FiltroDonaciones(null, null, null, null, false), primeraPagina);

            assertThat(ids(pagina)).containsExactly(
                    arrozDeBetoAsignado.getId(), ropaDeAna.getId(), arrozDeAna.getId());
            assertThat(pagina.totalElementos()).isEqualTo(3);
        }

        @Test
        @DisplayName("Filtra por estado")
        void porEstado() {
            Pagina<Donacion> pagina = repositorio.buscar(
                    new FiltroDonaciones("EN_DEPOSITO", null, null, null, false), primeraPagina);

            assertThat(ids(pagina)).containsExactlyInAnyOrder(arrozDeAna.getId(), ropaDeAna.getId());
        }

        @Test
        @DisplayName("Filtra por persona donante")
        void porDonante() {
            Pagina<Donacion> pagina = repositorio.buscar(new FiltroDonaciones(null, beto, null, null, false), primeraPagina);

            assertThat(ids(pagina)).containsExactly(arrozDeBetoAsignado.getId());
        }

        @Test
        @DisplayName("Filtra por subcategoría sin distinguir mayúsculas ni espacios")
        void porSubcategoria() {
            Pagina<Donacion> pagina = repositorio.buscar(
                    new FiltroDonaciones(null, null, "  ARROZ ", null, false), primeraPagina);

            assertThat(ids(pagina)).containsExactlyInAnyOrder(arrozDeAna.getId(), arrozDeBetoAsignado.getId());
        }

        @Test
        @DisplayName("Filtra por entidad beneficiaria asignada")
        void porEntidad() {
            Pagina<Donacion> pagina = repositorio.buscar(new FiltroDonaciones(null, null, null, comedor, false), primeraPagina);

            assertThat(ids(pagina)).containsExactly(arrozDeBetoAsignado.getId());
        }

        @Test
        @DisplayName("Los filtros se combinan: la donación tiene que cumplir todos")
        void combinados() {
            Pagina<Donacion> pagina = repositorio.buscar(
                    new FiltroDonaciones("EN_DEPOSITO", ana, "arroz", null, false), primeraPagina);

            assertThat(ids(pagina)).containsExactly(arrozDeAna.getId());
        }

        @Test
        @DisplayName("Un filtro en blanco se ignora, igual que si no se hubiera enviado")
        void enBlancoSeIgnora() {
            Pagina<Donacion> pagina = repositorio.buscar(new FiltroDonaciones("  ", null, "  ", null, false), primeraPagina);

            assertThat(pagina.totalElementos()).isEqualTo(3);
        }

        @Test
        @DisplayName("Las donaciones leídas traen sus bienes y su estado")
        void traeBienesYEstado() {
            Donacion leida = repositorio.buscar(new FiltroDonaciones(null, beto, null, null, false), primeraPagina)
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

            Pagina<Donacion> primera = repositorio.buscar(new FiltroDonaciones(null, null, null, null, false), new SolicitudPagina(0, 2));
            Pagina<Donacion> tercera = repositorio.buscar(new FiltroDonaciones(null, null, null, null, false), new SolicitudPagina(2, 2));

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
         * Cada asignación parte de la donación leída de la base y la modifica en la
         * misma sesión, como el service dentro de su transacción. Guardar dos veces
         * el mismo objeto desprendido falla por el bloqueo optimista (su versión
         * quedó vieja después del primer guardado).
         */
        private void asignar(Donacion donacion, UUID idEntidad, LocalDate fecha) {
            Donacion actual = repositorio.buscarPorId(donacion.getId()).orElseThrow();
            asignarEn(actual, idEntidad, fecha);
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
            actual.darDeBaja();
            repositorio.guardar(actual);
            jpa.flush();
            em.clear();

            assertThat(repositorio.buscarPorId(donacion.getId())).isEmpty();
            assertThat(repositorio.obtenerLoteEnEstado("ASIGNACION_REALIZADA", null, 100)).extracting(Donacion::getId)
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
            asignarEn(gestionada, comedor, hoy);
            repositorio.guardar(gestionada);
            jpa.flush();
            assertThat(recibidas(comedor)).isEqualTo(1);

            asignarEn(gestionada, otraEntidad, hoy);
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

    @Nested
    @DisplayName("Lotes de los procesos nocturnos")
    class Lotes {

        @Test
        @DisplayName("Recorre por id las de un estado: cada lote sigue desde la última del anterior, sin repetir")
        void recorrePorId() {
            List<Donacion> enDeposito = List.of(
                    donacion(ana, "Arroz", LocalDateTime.of(2026, 10, 1, 10, 0)),
                    donacion(ana, "Ropa", LocalDateTime.of(2026, 10, 1, 11, 0)),
                    donacion(beto, "Leche", LocalDateTime.of(2026, 10, 1, 12, 0)));
            Donacion asignada = donacion(beto, "Arroz", LocalDateTime.of(2026, 10, 1, 13, 0));
            asignada.asignarA(entidad(comedor));
            List<Donacion> todas = new ArrayList<>(enDeposito);
            todas.add(asignada);
            persistirYLimpiar(todas);

            List<Donacion> primero = repositorio.obtenerLoteEnEstado("EN_DEPOSITO", null, 2);
            List<Donacion> segundo = repositorio.obtenerLoteEnEstado("EN_DEPOSITO", primero.getLast().getId(), 2);
            List<Donacion> tercero = repositorio.obtenerLoteEnEstado("EN_DEPOSITO", segundo.getLast().getId(), 2);

            assertThat(primero).hasSize(2);
            assertThat(segundo).hasSize(1);
            assertThat(tercero).isEmpty();
            assertThat(Stream.concat(primero.stream(), segundo.stream()).map(Donacion::getId))
                    .containsExactlyInAnyOrderElementsOf(enDeposito.stream().map(Donacion::getId).toList());
        }
    }
}
