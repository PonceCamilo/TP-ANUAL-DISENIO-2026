package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.UbicacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.exceptions.CamionNoEncontradoException;
import ar.utn.donatrack.logistica.exceptions.OperacionCamionIlegalException;
import ar.utn.donatrack.logistica.exceptions.PatenteDuplicadaException;
import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la flota: alta, disponibilidad y última ubicación.
 * Se mockean el repositorio de camiones y el de rutas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CamionService - alta y consulta de la flota")
class CamionServiceTest {

    @Mock
    private CamionRepositoryInterface repositorio;

    @Mock
    private RutaRepositoryInterface rutaRepositorio;

    @Captor
    private ArgumentCaptor<Camion> camionCaptor;

    private CamionService servicio;
    private final UUID idCamion = UUID.randomUUID();

    @BeforeEach
    void crearServicio() {
        servicio = new CamionService(repositorio, rutaRepositorio);
    }

    private Camion camionEn(EstadoCamion estado) {
        return Camion.builder().id(idCamion).patente("AB123CD").estado(estado).build();
    }

    private UbicacionRequestDTO ubicacion() {
        UbicacionRequestDTO dto = new UbicacionRequestDTO();
        dto.setLatitud(-34.6);
        dto.setLongitud(-58.4);
        return dto;
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

    @Nested
    @DisplayName("Disponibilidad")
    class Disponibilidad {

        @Test
        @DisplayName("Un camión DISPONIBLE sin ruta abierta pasa a MANTENIMIENTO")
        void pasaAMantenimiento() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(camionEn(EstadoCamion.DISPONIBLE));

            CamionResponseDTO resultado = servicio.pasarAMantenimiento(idCamion);

            assertThat(resultado.getEstado()).isEqualTo(EstadoCamion.MANTENIMIENTO);
            verify(repositorio).guardar(camionCaptor.capture());
            assertThat(camionCaptor.getValue().getEstado()).isEqualTo(EstadoCamion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("Una ruta ya FINALIZADA no impide el mantenimiento")
        void rutaFinalizadaNoImpide() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(camionEn(EstadoCamion.DISPONIBLE));
            when(rutaRepositorio.buscarPorCamionId(idCamion)).thenReturn(List.of(
                    Ruta.builder().estado(EstadoRuta.FINALIZADA).build()));

            assertThat(servicio.pasarAMantenimiento(idCamion).getEstado()).isEqualTo(EstadoCamion.MANTENIMIENTO);
        }

        @Test
        @DisplayName("Rechaza el mantenimiento si el camión está EN_RUTA")
        void rechazaEnRuta() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(camionEn(EstadoCamion.EN_RUTA));

            assertThatThrownBy(() -> servicio.pasarAMantenimiento(idCamion))
                    .isInstanceOf(OperacionCamionIlegalException.class);
            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Rechaza el mantenimiento si hay una ruta PLANIFICADA")
        void rechazaRutaPlanificada() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(camionEn(EstadoCamion.DISPONIBLE));
            when(rutaRepositorio.buscarPorCamionId(idCamion)).thenReturn(List.of(
                    Ruta.builder().estado(EstadoRuta.PLANIFICADA).build()));

            assertThatThrownBy(() -> servicio.pasarAMantenimiento(idCamion))
                    .isInstanceOf(OperacionCamionIlegalException.class);
            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Volver a disponible solo sale de MANTENIMIENTO")
        void vuelveADisponible() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(camionEn(EstadoCamion.MANTENIMIENTO));

            assertThat(servicio.volverADisponible(idCamion).getEstado()).isEqualTo(EstadoCamion.DISPONIBLE);
        }

        @Test
        @DisplayName("No vuelve a disponible si ya lo está")
        void yaDisponible() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(camionEn(EstadoCamion.DISPONIBLE));

            assertThatThrownBy(() -> servicio.volverADisponible(idCamion))
                    .isInstanceOf(OperacionCamionIlegalException.class);
            verify(repositorio, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("Última ubicación")
    class Ubicacion {

        @Test
        @DisplayName("Con el camión EN_RUTA guarda el punto y la hora del servidor")
        void registraPunto() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(camionEn(EstadoCamion.EN_RUTA));
            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

            CamionResponseDTO resultado = servicio.registrarUbicacion(idCamion, ubicacion());

            assertThat(resultado.getLatitud()).isEqualTo(-34.6);
            assertThat(resultado.getLongitud()).isEqualTo(-58.4);
            assertThat(resultado.getUbicacionActualizadaEn()).isAfter(antes);
            verify(repositorio).guardar(camionCaptor.capture());
            assertThat(camionCaptor.getValue().getLatitud()).isEqualTo(-34.6);
        }

        @Test
        @DisplayName("Rechaza la ubicación si el camión no está en ruta")
        void rechazaSiNoEstaEnRuta() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(camionEn(EstadoCamion.DISPONIBLE));

            assertThatThrownBy(() -> servicio.registrarUbicacion(idCamion, ubicacion()))
                    .isInstanceOf(OperacionCamionIlegalException.class);
            verify(repositorio, never()).guardar(any());
        }

        @Test
        @DisplayName("Responde que no existe si el id no está en la flota")
        void camionInexistente() {
            when(repositorio.buscarPorId(idCamion)).thenReturn(null);

            assertThatThrownBy(() -> servicio.registrarUbicacion(idCamion, ubicacion()))
                    .isInstanceOf(CamionNoEncontradoException.class);
        }
    }
}
