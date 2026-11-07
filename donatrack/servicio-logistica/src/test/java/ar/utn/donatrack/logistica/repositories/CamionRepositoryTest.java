package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ODM de camiones contra el MongoDB embebido de los tests.
 * El índice único de patente se vuelve a crear antes de cada caso.
 */
@DataMongoTest
@Import(CamionRepository.class)
@TestPropertySource(properties = "spring.data.mongodb.auto-index-creation=true")
@DisplayName("Repositorio Mongo de camiones")
class CamionRepositoryTest {

    @Autowired private CamionRepository camiones;
    @Autowired private MongoTemplate mongo;

    @BeforeEach
    void limpiar() {
        mongo.dropCollection(Camion.class);
        mongo.indexOps(Camion.class).ensureIndex(
                new Index().on("patente", Sort.Direction.ASC).unique());
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

    @Test
    @DisplayName("guarda el documento y lo lee por id, con el UUID y el estado por defecto")
    void guardaYLeePorId() {
        Camion camion = camion("AB123CD");

        Camion leido = camiones.buscarPorId(camion.getId());

        assertThat(leido.getId()).isEqualTo(camion.getId());
        assertThat(leido.getPatente()).isEqualTo("AB123CD");
        assertThat(leido.getEstado()).isEqualTo(EstadoCamion.DISPONIBLE);
        assertThat(leido.getCapacidadVolumenM3()).isEqualTo(10.5);
        assertThat(leido.getAlturaM()).isEqualTo(2.5);
        assertThat(leido.getCapacidadCargaKg()).isEqualTo(3000);
    }

    @Test
    @DisplayName("buscarPorIds respeta el orden pedido e ignora ids nulos, repetidos o inexistentes")
    void buscarPorIdsRespetaOrden() {
        Camion a = camion("AA111AA");
        Camion b = camion("BB222BB");

        List<UUID> pedidos = new ArrayList<>();
        pedidos.add(b.getId());
        pedidos.add(null);
        pedidos.add(UUID.randomUUID());
        pedidos.add(a.getId());
        pedidos.add(b.getId());

        assertThat(camiones.buscarPorIds(pedidos)).extracting(Camion::getId).containsExactly(b.getId(), a.getId());
        assertThat(camiones.buscarTodos()).extracting(Camion::getPatente).containsExactly("AA111AA", "BB222BB");
    }

    @Test
    @DisplayName("buscarPorPatente encuentra el camión o devuelve null")
    void buscarPorPatente() {
        Camion camion = camion("CC333CC");

        assertThat(camiones.buscarPorPatente("CC333CC").getId()).isEqualTo(camion.getId());
        assertThat(camiones.buscarPorPatente("ZZ999ZZ")).isNull();
    }

    @Test
    @DisplayName("guardar el mismo id actualiza el documento y no crea otro")
    void guardarMismoIdActualiza() {
        Camion camion = camion("DD444DD");
        camion.setEstado(EstadoCamion.MANTENIMIENTO);
        camiones.guardar(camion);

        assertThat(camiones.buscarTodos()).hasSize(1);
        assertThat(camiones.buscarPorId(camion.getId()).getEstado()).isEqualTo(EstadoCamion.MANTENIMIENTO);
    }

    @Test
    @DisplayName("el índice único rechaza una segunda patente igual")
    void patenteUnica() {
        camion("EE555EE");
        Camion repetido = Camion.builder()
                .id(UUID.randomUUID())
                .patente("EE555EE")
                .capacidadVolumenM3(1)
                .alturaM(1)
                .capacidadCargaKg(1)
                .build();

        assertThatThrownBy(() -> camiones.guardar(repetido)).isInstanceOf(DuplicateKeyException.class);
        assertThat(camiones.buscarTodos()).hasSize(1);
    }
}
