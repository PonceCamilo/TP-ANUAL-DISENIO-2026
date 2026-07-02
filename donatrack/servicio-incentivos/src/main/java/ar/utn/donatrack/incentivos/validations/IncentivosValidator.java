package ar.utn.donatrack.incentivos.validations;

import ar.utn.donatrack.incentivos.exception.CategoriasDonadasInvalidasException;
import ar.utn.donatrack.incentivos.exception.MisionNoEncontradaException;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncentivosValidator {

    public void validarCategoriasDonadas(List<String> categoriasDonadas) {
        if (categoriasDonadas == null || categoriasDonadas.isEmpty()) {
            throw new CategoriasDonadasInvalidasException();
        }

        boolean hayCategoriaInvalida = categoriasDonadas.stream()
                .anyMatch(categoria -> categoria == null || categoria.isBlank());

        if (hayCategoriaInvalida) {
            throw new CategoriasDonadasInvalidasException();
        }
    }

    public void validarMisionesDisponibles(List<Mision> misiones, CategoriaDonante categoria) {
        if (misiones == null || misiones.isEmpty()) {
            throw new MisionNoEncontradaException(categoria.getClass().getSimpleName());
        }
    }
}
