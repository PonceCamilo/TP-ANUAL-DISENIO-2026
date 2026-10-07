package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.exceptions.CamionNoEncontradoException;
import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del CRUD de la flota.
 *
 * Cubren el alta (el camión nace DISPONIBLE) y la consulta por id / listado.
 * Solo se mockea el repositorio: el service no tiene más colaboradores.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CamionService - alta y consulta de la flota")
class CamionServiceTest {

    @Mock
    private CamionRepositoryInterface repositorio;

    @Captor
    private ArgumentCaptor<Camion> camionCaptor;

    private CamionService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new CamionService(repositorio);
    }

    private CamionRequestDTO dtoAlta() {
        CamionRequestDTO dto = new CamionRequestDTO();
        dto.setPatente("AB123CD");
        dto.setCapacidadVolumenM3(20.0);
        dto.setAlturaM(2.5);
        dto.setCapacidadCargaKg(1500.0);
        return dto;
    }

    @Nested
    @DisplayName("Alta de camiones")
    class Alta {

        @Test
        @DisplayName("Registra el camión con id generado, patente y estado DISPONIBLE")
        void registraDisponible() {
            CamionResponseDTO resultado = servicio.registrar(dtoAlta());

            assertThat(resultado.getPatente()).isEqualTo("AB123CD");
            assertThat(resultado.getEstado()).isEqualTo(EstadoCamion.DISPONIBLE);
            assertThat(resultado.getId()).isNotNull();

            verify(repositorio).guardar(camionCaptor.capture());
            assertThat(camionCaptor.getValue().getCapacidadVolumenM3()).isEqualTo(20.0);
            assertThat(camionCaptor.getValue().getCapacidadCargaKg()).isEqualTo(1500.0);
            assertThat(camionCaptor.getValue().getEstado()).isEqualTo(EstadoCamion.DISPONIBLE);
        }
    }

    @Nested
    @DisplayName("Consulta de camiones")
    class Consulta {

        @Test
        @DisplayName("obtenerPorId() devuelve el camión pedido")
        void obtenerPorId() {
            UUID id = UUID.randomUUID();
            Camion camion = Camion.builder().id(id).patente("AB123CD").build();
            when(repositorio.buscarPorId(id)).thenReturn(camion);

            CamionResponseDTO resultado = servicio.obtenerPorId(id);

            assertThat(resultado.getId()).isEqualTo(id);
            assertThat(resultado.getPatente()).isEqualTo("AB123CD");
        }

        @Test
        @DisplayName("obtenerPorId() lanza 404 si el camión no existe")
        void obtenerPorIdInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(repositorio.buscarPorId(idInexistente)).thenReturn(null);

            assertThatThrownBy(() -> servicio.obtenerPorId(idInexistente))
                    .isInstanceOf(CamionNoEncontradoException.class);
        }

        @Test
        @DisplayName("obtenerTodos() delega en el repositorio")
        void obtenerTodos() {
            Camion camion = Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build();
            when(repositorio.buscarTodos()).thenReturn(List.of(camion));

            List<CamionResponseDTO> resultado = servicio.obtenerTodos();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.getFirst().getPatente()).isEqualTo("AB123CD");
        }
    }
}
