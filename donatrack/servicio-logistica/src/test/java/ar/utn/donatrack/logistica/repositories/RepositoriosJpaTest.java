package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.CambioEstadoEntrega;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.entrega.MotivoFalloEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import ar.utn.donatrack.logistica.models.planificacion.DonacionLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import ar.utn.donatrack.logistica.models.planificacion.Parada;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de persistencia sobre una base H2 en memoria: el esquema lo crean las
 * mismas migraciones de Flyway que en producción, y Hibernate lo valida contra
 * las entidades. Cada test hace flush + clear antes de leer, para que las
 * lecturas vayan a la base y no al contexto de persistencia.
 */
@DataJpaTest
@Import({CamionRepository.class, EntregaRepository.class, RutaRepository.class, LotePlanificacionRepository.class})
@DisplayName("Repositorios JPA de logística")
class RepositoriosJpaTest {

    @Autowired private TestEntityManager tem;
    @Autowired private CamionRepository camiones;
    @Autowired private EntregaRepository entregas;
    @Autowired private RutaRepository rutas;
    @Autowired private LotePlanificacionRepository lotes;

    private Camion camion(String patente) {
        Camion camion = Camion.builder()
                .id(UUID.randomUUID())
                .patente(patente)
                .capacidadVolumenM3(10.5)
                .alturaM(2.5)
                .capacidadCargaKg(3000)
                .build();
        camiones.guardar(camion);
        return camion;
    }

    private Direccion direccion() {
        return Direccion.builder()
                .calle("Av. Medrano").numero(951).localidad("CABA")
                .provincia("Buenos Aires").codigoPostal("C1179")
                .build();
    }

    private LotePlanificacion lote(Camion camion, UUID idDonacion, UUID idEntidad) {
        LotePlanificacion lote = LotePlanificacion.builder()
                .id(UUID.randomUUID())
                .camiones(List.of(camion))
                .donaciones(List.of(DonacionLote.builder()
                        .idDonacion(idDonacion)
                        .idEntidadBeneficiaria(idEntidad)
                        .direccionEntrega(direccion())
                        .build()))
                .estado(EstadoLote.ENVIADO)
                .tokenCorrelacion(UUID.randomUUID().toString())
                .fechaEnvio(LocalDateTime.now())
                .build();
        lotes.guardar(lote);
        return lote;
    }

    /**
     * Arma lote + Ruta → Parada → Entrega en el mismo orden que
     * PlanificacionRutasService: la ruta (que guarda en cascada paradas y
     * entregas) y después la entrega en su repositorio.
     */
    private record EntregaYRuta(Entrega entrega, Ruta ruta) {
    }

    private Entrega entregaEnRutaNueva(Camion camion) {
        return entregaYRutaNuevas(camion).entrega();
    }

    private EntregaYRuta entregaYRutaNuevas(Camion camion) {
        UUID idDonacion = UUID.randomUUID();
        UUID idEntidad = UUID.randomUUID();
        LotePlanificacion lote = lote(camion, idDonacion, idEntidad);

        List<Parada> paradas = new ArrayList<>();
        Ruta ruta = Ruta.builder()
                .id(UUID.randomUUID()).lote(lote).camion(camion)
                .paradas(paradas).estado(EstadoRuta.PLANIFICADA)
                .build();
        List<Entrega> entregasParada = new ArrayList<>();
        Parada parada = Parada.builder()
                .id(UUID.randomUUID()).orden(1).direccion(direccion())
                .idEntidadBeneficiaria(idEntidad).entregas(entregasParada)
                .build();
        paradas.add(parada);
        Entrega entrega = Entrega.builder()
                .id(UUID.randomUUID()).idDonacion(idDonacion).parada(parada)
                .build();
        entregasParada.add(entrega);
        rutas.guardar(ruta);
        entregas.guardar(entrega);
        return new EntregaYRuta(entrega, ruta);
    }

    private void sincronizar() {
        tem.flush();
        tem.clear();
    }

    @Test
    @DisplayName("guarda el agregado Ruta → Parada → Entrega y lo encuentra por entrega y por camión")
    void guardaAgregadoYLoEncuentraPorEntregaYCamion() {
        Camion camion = camion("AB123CD");
        EntregaYRuta creadas = entregaYRutaNuevas(camion);
        Entrega entrega = creadas.entrega();
        UUID idRuta = creadas.ruta().getId();
        sincronizar();

        Ruta porEntrega = rutas.buscarPorEntregaId(entrega.getId()).orElseThrow();
        assertThat(porEntrega.getId()).isEqualTo(idRuta);
        assertThat(porEntrega.getCamion().getPatente()).isEqualTo("AB123CD");
        assertThat(porEntrega.obtenerEntregas()).extracting(Entrega::getId).containsExactly(entrega.getId());
        assertThat(porEntrega.getParadas().get(0).getDireccion().getCalle()).isEqualTo("Av. Medrano");

        Entrega leida = entregas.buscarPorId(entrega.getId());
        assertThat(leida.getParada().getId()).isEqualTo(porEntrega.getParadas().get(0).getId());

        assertThat(rutas.buscarPorCamionId(camion.getId())).extracting(Ruta::getId).containsExactly(idRuta);
        assertThat(rutas.buscarPorEntregaId(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("los cambios sobre una entrega gestionada (historial y fotos) se persisten")
    void persisteHistorialYFotosDeLaEntrega() {
        Entrega creada = entregaEnRutaNueva(camion("AD789GH"));
        sincronizar();

        Entrega entrega = entregas.buscarPorId(creada.getId());
        entrega.registrarCambio(EstadoEntrega.EN_TRASLADO, "Inicio de ruta");
        entrega.getFotosComprobante().addAll(List.of("https://fotos/1.jpg", "https://fotos/2.jpg"));
        entrega.registrarCambio(EstadoEntrega.NO_RECIBIDA, MotivoFalloEntrega.ENTIDAD_AUSENTE, null);
        entregas.guardar(entrega);
        sincronizar();

        Entrega leida = entregas.buscarPorId(entrega.getId());
        assertThat(leida.getEstado()).isEqualTo(EstadoEntrega.NO_RECIBIDA);
        assertThat(leida.getHistorial()).extracting(CambioEstadoEntrega::getEstado)
                .containsExactly(EstadoEntrega.EN_TRASLADO, EstadoEntrega.NO_RECIBIDA);
        assertThat(leida.getHistorial()).extracting(CambioEstadoEntrega::getMotivoFallo)
                .containsExactly(null, MotivoFalloEntrega.ENTIDAD_AUSENTE);
        assertThat(leida.getHistorial()).extracting(CambioEstadoEntrega::getObservacion)
                .containsExactly("Inicio de ruta", null);
        assertThat(leida.getFotosComprobante()).containsExactly("https://fotos/1.jpg", "https://fotos/2.jpg");

        assertThat(entregas.buscarPorEstado(EstadoEntrega.NO_RECIBIDA)).extracting(Entrega::getId)
                .containsExactly(entrega.getId());
        assertThat(entregas.buscarPorEstado(EstadoEntrega.ENTREGADA)).isEmpty();
    }

    @Test
    @DisplayName("guarda el lote con sus camiones y el snapshot de donaciones con su dirección")
    void guardaLoteConCamionesYDonaciones() {
        Camion camion = camion("AE111AA");
        LotePlanificacion lote = lote(camion, UUID.randomUUID(), UUID.randomUUID());
        sincronizar();

        LotePlanificacion leido = lotes.buscarPorId(lote.getId());
        assertThat(leido.getCamiones()).extracting(Camion::getPatente).containsExactly("AE111AA");
        assertThat(leido.getDonaciones()).hasSize(1);
        assertThat(leido.getDonaciones().get(0).getDireccionEntrega().getNumero()).isEqualTo(951);
        assertThat(leido.getDonaciones().get(0).getDireccionEntrega().getId()).isNotNull();
    }

    @Test
    @DisplayName("buscarPorIds respeta el orden pedido e ignora ids nulos, repetidos o inexistentes")
    void buscarPorIdsRespetaOrden() {
        Camion a = camion("AA111AA");
        Camion b = camion("BB222BB");
        sincronizar();

        List<UUID> pedidos = new ArrayList<>();
        pedidos.add(b.getId());
        pedidos.add(null);
        pedidos.add(UUID.randomUUID());
        pedidos.add(a.getId());
        pedidos.add(b.getId());

        assertThat(camiones.buscarPorIds(pedidos)).extracting(Camion::getId).containsExactly(b.getId(), a.getId());
        assertThat(camiones.buscarTodos()).extracting(Camion::getPatente).containsExactly("AA111AA", "BB222BB");
        assertThat(camiones.buscarPorId(a.getId()).getEstado()).isEqualTo(EstadoCamion.DISPONIBLE);
        assertThat(camiones.buscarPorId(a.getId()).getCapacidadVolumenM3()).isEqualTo(10.5);
    }

    @Test
    @DisplayName("buscarPorPatente encuentra el camión o devuelve null")
    void buscarPorPatente() {
        Camion camion = camion("CC333CC");
        sincronizar();

        assertThat(camiones.buscarPorPatente("CC333CC").getId()).isEqualTo(camion.getId());
        assertThat(camiones.buscarPorPatente("ZZ999ZZ")).isNull();
    }

    @Test
    @DisplayName("guardar un objeto desconectado con id existente actualiza en vez de duplicar")
    void guardarDesconectadoActualiza() {
        Camion camion = camion("DD444DD");
        sincronizar();

        camion.setEstado(EstadoCamion.MANTENIMIENTO);
        camiones.guardar(camion);
        sincronizar();

        assertThat(camiones.buscarTodos()).hasSize(1);
        assertThat(camiones.buscarPorId(camion.getId()).getEstado()).isEqualTo(EstadoCamion.MANTENIMIENTO);
    }
}
