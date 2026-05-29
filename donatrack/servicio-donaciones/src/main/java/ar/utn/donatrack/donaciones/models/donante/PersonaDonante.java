package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.models.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

/**
 * Raíz de la jerarquía de personas donantes.
 * Una persona donante puede ser humana o jurídica.
 *
 * PATRÓN STATE aplicado: el ciclo de vida se modela con EstadoDonante en lugar
 * de un boolean activo. Ver EstadoDonante.java para la justificación completa.
 */

@SuperBuilder
@Getter
@Setter
public abstract class PersonaDonante {

    protected String tipoDocumento;
    protected String numeroDocumento;
    protected Direccion direccion;
    protected EstadoDonante estado;

}

