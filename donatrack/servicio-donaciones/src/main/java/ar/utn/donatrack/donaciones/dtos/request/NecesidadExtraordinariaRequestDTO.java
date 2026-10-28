package ar.utn.donatrack.donaciones.dtos.request;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.TipoNecesidad;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class NecesidadExtraordinariaRequestDTO extends NecesidadRequestDTO {

    @Override
    public TipoNecesidad tipo() {
        return TipoNecesidad.EXTRAORDINARIA;
    }

    @Override
    public Necesidad crear(LocalDate hoy) {
        return completar(new NecesidadExtraordinaria(), hoy);
    }
}
