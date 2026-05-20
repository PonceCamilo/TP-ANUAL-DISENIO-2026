package ar.utn.donatrack.donaciones.model.donante;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Persona donante de tipo jurídico (empresa, ONG, institución, etc.).
 * Debe contar con al menos un representante habilitado para operar.
 */

@SuperBuilder
@Getter
@Setter
public class PersonaJuridicaDonante extends PersonaDonante {

    private String razonSocial;
    private TipoPersonaJuridica tipo;
    private String rubro;
    private final List<Representante> representantes;

    // ─── Métodos de negocio ───────────────────────────────────────────────────

    public void agregarRepresentante(Representante representante) {
        if (representante == null)
            throw new IllegalArgumentException("El representante no puede ser nulo.");
        boolean yaExiste = representantes.stream()
                .anyMatch(r -> r.getEmail().equalsIgnoreCase(representante.getEmail()));
        if (yaExiste) {
            throw new IllegalArgumentException(
                    "Ya existe un representante con el email: " + representante.getEmail());
        }
        representantes.add(representante);
    }

    public void removerRepresentante(String emailRepresentante) {
        boolean removed = representantes.removeIf(
                r -> r.getEmail().equalsIgnoreCase(emailRepresentante));
        if (!removed) {
            throw new IllegalArgumentException(
                    "No se encontró un representante con el email: " + emailRepresentante);
        }
        if (representantes.isEmpty()) {
            throw new IllegalStateException(
                    "La persona jurídica debe tener al menos un representante.");
        }
    }
}
