package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.models.contacto.Email;
import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.estado.EstadoDonante;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Raíz de la jerarquía de personas donantes.
 * Una persona donante puede ser humana (PersonaHumana) o jurídica (PersonaJuridica).
 * El ciclo de vida se modela con el enum EstadoDonante (ACTIVO, INACTIVO, BLOQUEADO);
 * las transiciones válidas se validan en PersonasValidator.
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
    protected MedioDeContacto medioContactoPredeterminado;
    protected LocalDateTime ultimaInteraccion;
    protected String email;

    @Builder.Default
    protected List<MedioDeContacto> contactos = new ArrayList<>();

    public void cambiarEstado(String estadoDestino, String justificacion) {
        this.estado = this.estado.transicionarA(estadoDestino, justificacion);
    }

    public String obtenerEmail() {
        return this.email;
    }

    public void registrarInteraccion() {
        this.ultimaInteraccion = FechaHoraArgentina.ahora();
    }

    public boolean estaInactivoDesde(LocalDateTime fechaLimite) {
        return this.ultimaInteraccion == null || this.ultimaInteraccion.isBefore(fechaLimite);
    }
}
