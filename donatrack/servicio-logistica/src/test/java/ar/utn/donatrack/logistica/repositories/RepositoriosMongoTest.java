package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.config.IndicesDocumentales;
import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.CambioEstadoEntrega;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.entrega.MotivoFalloEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.DonacionLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import ar.utn.donatrack.logistica.models.planificacion.Parada;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest
@Import({
        CamionRepository.class,
        EntregaRepository.class,
        RutaRepository.class,
        LotePlanificacionRepository.class,
        IndicesDocumentales.class
})
@DisplayName("Documentos de lotes, rutas y entregas")
class RepositoriosMongoTest {

    @Autowired private MongoTemplate mongo;
    @Autowired private CamionRepository camiones;
    @Autowired private EntregaRepository entregas;
    @Autowired private RutaRepository rutas;
    @Autowired private LotePlanificacionRepository lotes;

    @BeforeEach
    void limpiar() {
        mongo.dropCollection(Camion.class);
        mongo.dropCollection(LotePlanificacion.class);
        mongo.dropCollection(Ruta.class);
        mongo.indexOps(Camion.class).ensureIndex(
                new org.springframework.data.mongodb.core.index.Index()
                        .on("patente", org.springframework.data.domain.Sort.Direction.ASC).unique());
        IndicesDocumentales.asegurar(mongo);
    }

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

    private Ruta rutaConEntrega(Camion camion) {
        UUID idDonacion = UUID.randomUUID();
        UUID idEntidad = UUID.randomUUID();
        LotePlanificacion lote = LotePlanificacion.builder()
                .id(UUID.randomUUID())
                .camionIds(List.of(camion.getId()))
                .donaciones(List.of(DonacionLote.builder()
                        .idDonacion(idDonacion)
                        .idEntidadBeneficiaria(idEntidad)
                        .direccionEntrega(direccion())
                        .build()))
                .estado(EstadoLote.COMPLETADO)
                .tokenCorrelacion(UUID.randomUUID().toString())
                .fechaEnvio(LocalDateTime.now())
                .build();
        lotes.guardar(lote);

        List<Entrega> entregasDeLaParada = new ArrayList<>();
        Parada parada = Parada.builder()
                .id(UUID.randomUUID())
                .orden(1)
                .direccion(direccion())
                .idEntidadBeneficiaria(idEntidad)
                .entregas(entregasDeLaParada)
                .build();
        Entrega entrega = Entrega.builder()
                .id(UUID.randomUUID())
                .idDonacion(idDonacion)
                .parada(parada)
                .build();
        entregasDeLaParada.add(entrega);

        Ruta ruta = Ruta.builder()
                .id(UUID.randomUUID())
                .loteId(lote.getId())
                .camion(camion)
                .paradas(new ArrayList<>(List.of(parada)))
                .estado(EstadoRuta.PLANIFICADA)
                .build();
        rutas.guardar(ruta);
        entregas.guardar(entrega);
        return ruta;
    }

    @Test
    @DisplayName("la ruta embebe parada y entrega, y al leerla vuelve a traer el camión")
    void guardaRutaYLaEncuentra() {
        Camion camion = camion("AB123CD");
        Ruta creada = rutaConEntrega(camion);
        UUID entregaId = creada.obtenerEntregas().getFirst().getId();

        Ruta leida = rutas.buscarPorEntregaId(entregaId).orElseThrow();

        assertThat(leida.getId()).isEqualTo(creada.getId());
        assertThat(leida.getLoteId()).isEqualTo(creada.getLoteId());
        assertThat(leida.getCamionId()).isEqualTo(camion.getId());
        assertThat(leida.getCamion().getPatente()).isEqualTo("AB123CD");
        assertThat(leida.getParadas().getFirst().getDireccion().getCalle()).isEqualTo("Av. Medrano");
        assertThat(leida.obtenerEntregas()).extracting(Entrega::getId).containsExactly(entregaId);

        Entrega entrega = entregas.buscarPorId(entregaId);
        assertThat(entrega.getParada().getId()).isEqualTo(leida.getParadas().getFirst().getId());
        assertThat(entregas.buscarPorIdDonacion(entrega.getIdDonacion())).extracting(Entrega::getId)
                .containsExactly(entregaId);
        assertThat(rutas.buscarPorCamionId(camion.getId())).extracting(Ruta::getId)
                .containsExactly(creada.getId());
    }

    @Test
    @DisplayName("guardar una entrega reescribe el historial y las fotos dentro de la ruta")
    void persisteHistorialYFotos() {
        Ruta ruta = rutaConEntrega(camion("AD789GH"));
        UUID entregaId = ruta.obtenerEntregas().getFirst().getId();

        Entrega entrega = entregas.buscarPorId(entregaId);
        entrega.registrarCambio(EstadoEntrega.EN_TRASLADO, "Inicio de ruta");
        entrega.getFotosComprobante().addAll(List.of("https://fotos/1.jpg", "https://fotos/2.jpg"));
        entrega.registrarCambio(EstadoEntrega.NO_RECIBIDA, MotivoFalloEntrega.ENTIDAD_AUSENTE, null);
        entregas.guardar(entrega);

        Entrega leida = entregas.buscarPorId(entregaId);
        assertThat(leida.getEstado()).isEqualTo(EstadoEntrega.NO_RECIBIDA);
        assertThat(leida.getHistorial()).extracting(CambioEstadoEntrega::getEstado)
                .containsExactly(EstadoEntrega.EN_TRASLADO, EstadoEntrega.NO_RECIBIDA);
        assertThat(leida.getHistorial()).extracting(CambioEstadoEntrega::getMotivoFallo)
                .containsExactly(null, MotivoFalloEntrega.ENTIDAD_AUSENTE);
        assertThat(leida.getFotosComprobante()).containsExactly("https://fotos/1.jpg", "https://fotos/2.jpg");
        assertThat(entregas.buscarPorEstado(EstadoEntrega.NO_RECIBIDA)).extracting(Entrega::getId)
                .containsExactly(entregaId);

        Ruta rutaLeida = rutas.buscarPorId(ruta.getId());
        assertThat(rutaLeida.obtenerEntregas().getFirst().getEstado()).isEqualTo(EstadoEntrega.NO_RECIBIDA);
    }

    @Test
    @DisplayName("el lote embebe la donación con su dirección y los ids de camión")
    void guardaLote() {
        Camion camion = camion("AE111AA");
        LotePlanificacion lote = LotePlanificacion.builder()
                .id(UUID.randomUUID())
                .camionIds(List.of(camion.getId()))
                .donaciones(List.of(DonacionLote.builder()
                        .idDonacion(UUID.randomUUID())
                        .idEntidadBeneficiaria(UUID.randomUUID())
                        .direccionEntrega(direccion())
                        .build()))
                .estado(EstadoLote.ENVIADO)
                .tokenCorrelacion(UUID.randomUUID().toString())
                .fechaEnvio(LocalDateTime.now())
                .build();
        lotes.guardar(lote);

        LotePlanificacion leido = lotes.buscarPorId(lote.getId());
        assertThat(leido.getCamionIds()).containsExactly(camion.getId());
        assertThat(leido.getDonaciones()).hasSize(1);
        assertThat(leido.getDonaciones().getFirst().getDireccionEntrega().getNumero()).isEqualTo(951);
        assertThat(leido.getDonaciones().getFirst().getDireccionEntrega().getCalle()).isEqualTo("Av. Medrano");
    }

    @Test
    @DisplayName("dos lotes no pueden compartir el token de correlación")
    void tokenUnico() {
        String token = UUID.randomUUID().toString();
        lotes.guardar(LotePlanificacion.builder()
                .id(UUID.randomUUID())
                .estado(EstadoLote.ENVIADO)
                .tokenCorrelacion(token)
                .fechaEnvio(LocalDateTime.now())
                .build());

        assertThatThrownBy(() -> lotes.guardar(LotePlanificacion.builder()
                .id(UUID.randomUUID())
                .estado(EstadoLote.ENVIADO)
                .tokenCorrelacion(token)
                .fechaEnvio(LocalDateTime.now())
                .build())).isInstanceOf(DuplicateKeyException.class);
    }
}
