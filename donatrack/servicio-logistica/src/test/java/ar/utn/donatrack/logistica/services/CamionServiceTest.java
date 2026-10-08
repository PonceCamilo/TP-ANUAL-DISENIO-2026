package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.exceptions.PatenteDuplicadaException;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del CRUD de la flota.
 *
 * Cubren el alta: el camión nace DISPONIBLE y la patente no se repite.
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

        @Test
        @DisplayName("Rechaza una patente ya registrada (409) sin guardar nada")
        void rechazaPatenteDuplicada() {
            when(repositorio.buscarPorPatente("AB123CD"))
                    .thenReturn(Camion.builder().id(UUID.randomUUID()).patente("AB123CD").build());

            assertThatThrownBy(() -> servicio.registrar(dtoAlta()))
                    .isInstanceOf(PatenteDuplicadaException.class);
            verify(repositorio, never()).guardar(any());
        }
    }
}
