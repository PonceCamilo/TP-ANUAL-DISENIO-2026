package ar.utn.donatrack.donaciones.model.donante;

import ar.utn.donatrack.donaciones.model.contacto.MedioDeContacto;
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

       // ─── Métodos de negocio ──────────────────────────────────────────────────

    public void agregarMedioDeContacto(MedioDeContacto medio) {
        boolean yaExiste = mediosDeContacto.stream()
                .anyMatch(m -> m.getTipo() == medio.getTipo()
                        && m.getValor().equalsIgnoreCase(medio.getValor()));
        if (yaExiste) {
            throw new IllegalArgumentException(
                    "Ya existe un medio de contacto de tipo " + medio.getTipo()
                            + " con el valor '" + medio.getValor() + "'.");
        }
        mediosDeContacto.add(medio);
    }

    /**
     * Realiza la baja lógica de la persona donante.
     * PATRÓN STATE: valida la transición ACTIVO → INACTIVO.
     */
    public void darDeBaja() {
        // (STATE): la transición se valida comparando estados
        if (this.estado == EstadoDonante.INACTIVO) {                // ← transición de estado
            throw new IllegalStateException("La persona donante ya se encuentra dada de baja.");
        }
        this.estado = EstadoDonante.INACTIVO;                       // ← cambio de estado
    }

    /**
     * Reactiva una persona donante previamente dada de baja.
     * PATRÓN STATE: transición INACTIVO → ACTIVO.
     */
    public void reactivar() {
        // STATE: transición inversa explícita
        this.estado = EstadoDonante.ACTIVO;                         // ← cambio de estado
    }
}
