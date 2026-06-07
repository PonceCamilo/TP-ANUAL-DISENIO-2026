package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Raíz de la jerarquía de personas donantes.
 * Una persona donante puede ser humana (PersonaHumana) o jurídica (PersonaJuridica).
 *
 * PATRÓN STATE: el ciclo de vida se modela con EstadoDonante.
 *
 * Unificación de contactos: el email es obligatorio y se almacena directamente
 * como campo para ser la clave de idempotencia en importaciones CSV y búsquedas.
 * Los contactos adicionales (teléfono, WhatsApp) se agregan a la lista `contactos`.
 */

@SuperBuilder
@Getter
@Setter
public abstract class PersonaDonante {

    protected UUID id;
    protected String tipoDocumento;
    protected String numeroDocumento;
    protected Direccion direccion;
    protected EstadoDonante estado;
    protected String email;
    protected MedioDeContacto medioContactoPredeterminado;

    @Builder.Default
    protected List<MedioDeContacto> contactos = new ArrayList<>();
}
