package ar.utn.donatrack.logistica.services;

import ar.utn.donatrack.logistica.dtos.request.CamionRequestDTO;
import ar.utn.donatrack.logistica.dtos.response.CamionResponseDTO;
import ar.utn.donatrack.logistica.exceptions.CamionNoEncontradoException;
import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.services.CamionServiceInterface;
import ar.utn.donatrack.logistica.models.flota.Camion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CamionService implements CamionServiceInterface {

    private final CamionRepositoryInterface repositorio;

    @Override
    public CamionResponseDTO registrar(CamionRequestDTO dto) {
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

    private Camion buscarOFallar(UUID id) {
        Camion camion = repositorio.buscarPorId(id);
        if (camion == null) {
            throw new CamionNoEncontradoException(id);
        }
        return camion;
    }
}
