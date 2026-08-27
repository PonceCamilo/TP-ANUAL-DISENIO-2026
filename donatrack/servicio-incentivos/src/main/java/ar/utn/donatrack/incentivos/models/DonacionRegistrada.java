package ar.utn.donatrack.incentivos.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

@Getter
@Builder
public class DonacionRegistrada {
    private LocalDateTime fecha;
    private Set<String> categorias;
    private int cantidadBienes;
    private boolean exitosa;
    private String entidadBeneficiaria;

    public Set<String> getCategorias() {
        return categorias == null ? Collections.emptySet() : categorias;
    }
}
