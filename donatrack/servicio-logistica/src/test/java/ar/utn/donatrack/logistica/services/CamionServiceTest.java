package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.exceptions.CamionNoEncontradoException;
import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CamionServiceTest {

    @Mock
    private CamionRepositoryInterface repositorio;

    private CamionService service;

    @BeforeEach
    void setUp() {
        service = new CamionService(repositorio);
    }

    @Test
    @DisplayName("registrar() crea el camión con estado DISPONIBLE por defecto")
    void registrarCreaCamionDisponible() {
        CamionRequestDTO dto = new CamionRequestDTO();
        dto.setPatente("AB123CD");
        dto.setCapacidadVolumenM3(20.0);
        dto.setAlturaM(2.5);
        dto.setCapacidadCargaKg(1500.0);

        CamionResponseDTO resultado = service.registrar(dto);

        assertEquals("AB123CD", resultado.getPatente());
        assertEquals(EstadoCamion.DISPONIBLE, resultado.getEstado());

        ArgumentCaptor<Camion> captor = ArgumentCaptor.forClass(Camion.class);
        verify(repositorio).guardar(captor.capture());
        assertEquals(20.0, captor.getValue().getCapacidadVolumenM3());
        assertEquals(1500.0, captor.getValue().getCapacidadCargaKg());
    }

    @Test
    @DisplayName("obtenerPorId() lanza CamionNoEncontradoException si no existe")
    void obtenerPorIdInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(null);

        assertThrows(CamionNoEncontradoException.class, () -> service.obtenerPorId(id));
    }

    @Test
    @DisplayName("obtenerTodos() delega en el repositorio")
    void obtenerTodosDelegaEnRepositorio() {
        Camion camion = Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build();
        when(repositorio.buscarTodos()).thenReturn(List.of(camion));

        List<CamionResponseDTO> resultado = service.obtenerTodos();

        assertEquals(1, resultado.size());
        assertEquals("AB123CD", resultado.getFirst().getPatente());
    }
}
