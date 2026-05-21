package ar.utn.donatrack.donaciones.model.donante;

import ar.utn.donatrack.donaciones.model.contacto.TipoMedioContacto;
import ar.utn.donatrack.donaciones.model.entidad.Direccion;
import ar.utn.donatrack.donaciones.model.donacion.CargaDonacion;
import ar.utn.donatrack.donaciones.model.donacion.Donacion;

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

    protected UUID id;
    protected String tipoPersona;
    protected String tipoDocumento;
    protected String numeroDocumento;
    protected List<TipoMedioContacto> mediosDeContacto;
    protected CargaDonacion cargaDonacion;
    protected Direccion direccion;
    protected String email;
    protected String telefono;
    protected TipoMedioContacto medioContactoPredeterminado;
    protected EstadoDonante estado;
    protected List<Donacion> donaciones;
}
