package ar.utn.donatrack.donaciones.validations.donaciones;

import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionSinBienesException;
import ar.utn.donatrack.donaciones.interfaces.repositories.DonacionesRepositoryInterface;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DonacionesValidator {

    private final DonacionesRepositoryInterface repositorio;

    /** Recupera la donación o lanza la excepción de dominio si no existe. */
    public Donacion validarYObtenerDonacion(UUID id) {
        Donacion donacion = repositorio.obtenerPorId(id);
        if (donacion == null) {
            throw new DonacionNoEncontradaException(id);
        }
        return donacion;
    }

    /** Verifica que la donación tenga al menos un bien para poder modificarlo. */
    public void validarTieneBienes(Donacion donacion) {
        if (donacion.getBienes().isEmpty()) {
            throw new DonacionSinBienesException(donacion.getId());
        }
    }
}
