package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public abstract class Mision {
    private UUID id = UUID.randomUUID();
    private String nombre;
    private String descripcion;
    private CategoriaDonante categoriaRequerida;
    protected int objetivo;
    private int orden;
    protected Insignia insignia;

    public abstract int progresoActual(Donante donante);

    public int restante(Donante donante) {
        return Math.max(0, this.objetivo - progresoActual(donante));
    }

    public abstract boolean estaCompletada(Donante donante);
}