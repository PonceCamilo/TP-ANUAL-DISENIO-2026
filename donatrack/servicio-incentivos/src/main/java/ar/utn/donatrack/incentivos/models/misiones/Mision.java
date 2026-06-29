package ar.utn.donatrack.incentivos.models.misiones;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import lombok.Getter;
import java.util.UUID;

@Getter
public abstract class Mision {
    private UUID id = UUID.randomUUID();
    private String nombre;
    private String descripcion;
    private CategoriaDonante categoriaRequerida;
    protected int objetivo;
    protected Insignia insignia;

    // el constructor esta hecho asi porque al parecer los builders no se llevan muy bien con la herencia
    protected Mision(String nombre, String descripcion, CategoriaDonante categoriaRequerida,
                     int objetivo, Insignia insignia) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoriaRequerida = categoriaRequerida;
        this.objetivo = objetivo;
        this.insignia = insignia;
    }

    public InsigniaObtenida otorgarInsignia() {
        return new InsigniaObtenida(insignia, true);
    }

    public abstract int progresoActual(Donante donante);

    public int restante(Donante donante) {
        return Math.max(0, this.objetivo - progresoActual(donante));
    }

    public abstract boolean estaCompletada(Donante donante);
}
