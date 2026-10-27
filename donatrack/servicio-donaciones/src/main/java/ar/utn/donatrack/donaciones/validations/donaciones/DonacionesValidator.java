package ar.utn.donatrack.donaciones.validations.donaciones;

import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.RepositorioDonaciones;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DonacionesValidator {

    private final RepositorioDonaciones repositorio;

    /** Recupera la donación o lanza la excepción de dominio si no existe. */
    public Donacion validarYObtenerDonacion(UUID id) {
        Donacion donacion = repositorio.obtenerPorId(id);
        if (donacion == null) {
            throw new DonacionNoEncontradaException(id);
        }
        return donacion;
    }
}
