package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Raíz de la jerarquía de necesidades de una EntidadBeneficiaria.
 * Subclases: NecesidadExtraordinaria, NecesidadRecurrente.
 */

@Getter
@Setter
public abstract class Necesidad {

    protected UUID id = UUID.randomUUID();
    protected String nombre;
    protected String descripcion;
    protected LocalDate fechaRegistro;
    protected int cantidadObjetivo;
    protected int cantidadRecibida;

    public void recibirDonacion(int cantidad) {
        this.cantidadRecibida += cantidad;
    }

    public boolean esCompatibleCon(String subcategoria) {
        if (subcategoria == null || subcategoria.isBlank()) {
            return false;
        }
        String objetivo = subcategoria.toLowerCase();
        return contieneTexto(nombre, objetivo) || contieneTexto(descripcion, objetivo);
    }

    private boolean contieneTexto(String texto, String objetivo) {
        return texto != null && texto.toLowerCase().contains(objetivo);
    }

    public abstract boolean estaSatisfecha();
}
