package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.request.UbicacionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.exceptions.CamionNoEncontradoException;
import ar.utn.donatrack.logistica.exceptions.OperacionCamionIlegalException;
import ar.utn.donatrack.logistica.exceptions.PatenteDuplicadaException;
import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.services.CamionServiceInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CamionService implements CamionServiceInterface {

    private final CamionRepositoryInterface repositorio;
    private final RutaRepositoryInterface rutaRepositorio;

    @Override
    public CamionResponseDTO registrar(CamionRequestDTO dto) {
        if (repositorio.buscarPorPatente(dto.getPatente()) != null) {
            throw new PatenteDuplicadaException(dto.getPatente());
        }
        Camion camion = Camion.builder()
                .id(UUID.randomUUID())
                .patente(dto.getPatente())
                .capacidadVolumenM3(dto.getCapacidadVolumenM3())
                .alturaM(dto.getAlturaM())
                .capacidadCargaKg(dto.getCapacidadCargaKg())
                .build();
        repositorio.guardar(camion);
        return CamionResponseDTO.desde(camion);
    }

    @Override
    public List<CamionResponseDTO> obtenerTodos() {
        return repositorio.buscarTodos().stream().map(CamionResponseDTO::desde).toList();
    }

    @Override
    public CamionResponseDTO obtenerPorId(UUID id) {
        return CamionResponseDTO.desde(buscarOFallar(id));
    }

    @Override
    public CamionResponseDTO pasarAMantenimiento(UUID id) {
        Camion camion = buscarOFallar(id);
        if (camion.getEstado() == EstadoCamion.MANTENIMIENTO) {
            throw new OperacionCamionIlegalException("El camión ya está en mantenimiento.");
        }
        if (camion.getEstado() == EstadoCamion.EN_RUTA) {
            throw new OperacionCamionIlegalException(
                    "El camión está en ruta y no puede pasar a mantenimiento.");
        }
        exigirSinRutaAbierta(camion);
        camion.setEstado(EstadoCamion.MANTENIMIENTO);
        repositorio.guardar(camion);
        return CamionResponseDTO.desde(camion);
    }

    @Override
    public CamionResponseDTO volverADisponible(UUID id) {
        Camion camion = buscarOFallar(id);
        if (camion.getEstado() != EstadoCamion.MANTENIMIENTO) {
            throw new OperacionCamionIlegalException(
                    "Solo un camión en mantenimiento puede volver a disponible. Estado actual: "
                            + camion.getEstado() + ".");
        }
        exigirSinRutaAbierta(camion);
        camion.setEstado(EstadoCamion.DISPONIBLE);
        repositorio.guardar(camion);
        return CamionResponseDTO.desde(camion);
    }

    @Override
    public void eliminar(UUID id) {
        Camion camion = buscarOFallar(id);
        if (camion.getEstado() == EstadoCamion.EN_RUTA) {
            throw new OperacionCamionIlegalException(
                    "El camión está en ruta y no se puede eliminar.");
        }
        exigirSinRutaAbierta(camion);
        repositorio.eliminar(id);
    }

    @Override
    public CamionResponseDTO registrarUbicacion(UUID id, UbicacionRequestDTO dto) {
        Camion camion = buscarOFallar(id);
        if (camion.getEstado() != EstadoCamion.EN_RUTA) {
            throw new OperacionCamionIlegalException(
                    "Solo se registra la ubicación de un camión que está en ruta.");
        }
        camion.setLatitud(dto.getLatitud());
        camion.setLongitud(dto.getLongitud());
        camion.setUbicacionActualizadaEn(LocalDateTime.now());
        repositorio.guardar(camion);
        return CamionResponseDTO.desde(camion);
    }

    /**
     * Una ruta PLANIFICADA o INICIADA ya comprometió al camión. Sacarlo de
     * servicio dejaría ese reparto colgado de un camión que la planificación
     * siguiente no va a volver a elegir.
     */
    private void exigirSinRutaAbierta(Camion camion) {
        boolean abierta = rutaRepositorio.buscarPorCamionId(camion.getId()).stream()
                .anyMatch(ruta -> ruta.getEstado() != EstadoRuta.FINALIZADA);
        if (abierta) {
            throw new OperacionCamionIlegalException(
                    "El camión tiene una ruta abierta y no se puede cambiar su disponibilidad.");
        }
    }

    private Camion buscarOFallar(UUID id) {
        Camion camion = repositorio.buscarPorId(id);
        if (camion == null) {
            throw new CamionNoEncontradoException(id);
        }
        return camion;
    }
}
