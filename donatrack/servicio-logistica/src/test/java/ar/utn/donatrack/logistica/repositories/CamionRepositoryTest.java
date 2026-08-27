package ar.utn.donatrack.logistica.repositories;

import ar.utn.donatrack.logistica.models.flota.Camion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CamionRepositoryTest {

    @Test
    @DisplayName("buscarPorIds() ignora ids nulos en la lista en vez de lanzar NullPointerException")
    void buscarPorIdsIgnoraIdsNulos() {
        CamionRepository repositorio = new CamionRepository();
        Camion camion = Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build();
        repositorio.guardar(camion);

        List<UUID> idsConNull = Arrays.asList(camion.getId(), null);

        List<Camion> resultado = assertDoesNotThrow(() -> repositorio.buscarPorIds(idsConNull));

        assertEquals(1, resultado.size());
        assertEquals(camion.getId(), resultado.getFirst().getId());
    }

    @Test
    @DisplayName("buscarPorIds() ignora ids que no existen en el repositorio")
    void buscarPorIdsIgnoraIdsInexistentes() {
        CamionRepository repositorio = new CamionRepository();

        List<Camion> resultado = repositorio.buscarPorIds(List.of(UUID.randomUUID()));

        assertEquals(0, resultado.size());
    }
}
