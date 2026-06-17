package ar.utn.donatrack.donaciones.validations;

import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.CampaniaNoEncontradaException;
import ar.utn.donatrack.donaciones.exceptions.entidadesExceptions.EntidadBeneficiariaNoEncontradaException;
import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EntidadesBeneficiariasValidator {

    private final EntidadesBeneficiariasRepositoryInterface repositorio;

    public void validarExistenciaEntidad(UUID id) {
        if (!repositorio.existePorId(id)) {
            throw new EntidadBeneficiariaNoEncontradaException(id);
        }
    }

    public Campania validarYObtenerCampania(EntidadBeneficiaria entidad, UUID campaniaId) {
        return entidad.getCampanias().stream()
                .filter(c -> c.getIdCampania().equals(campaniaId))
                .findFirst()
                .orElseThrow(() -> new CampaniaNoEncontradaException(campaniaId, entidad.getId()));
    }

    public void validarFechasCampania(LocalDate inicio, LocalDate fin) {
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new FechasCampaniaInvalidasException();
        }
    }
}