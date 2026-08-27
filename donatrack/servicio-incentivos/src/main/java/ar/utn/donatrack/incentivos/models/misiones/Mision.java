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
    private CategoriaDonante categoriaRequerida; // falto agregar esto al diagrama.
    protected int objetivo;
    private int orden;  // esto se podria sacar.
    protected Insignia insignia;

    protected Mision(String nombre, String descripcion, CategoriaDonante categoriaRequerida, int objetivo, Insignia insignia) {
        this(nombre, descripcion, categoriaRequerida, objetivo, 0, insignia);
    }

    protected Mision(String nombre, String descripcion, CategoriaDonante categoriaRequerida, int objetivo, int orden, Insignia insignia) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoriaRequerida = categoriaRequerida;
        this.objetivo = objetivo;
        this.orden = orden;
        this.insignia = insignia;
    }

    public InsigniaObtenida otorgarInsignia() {           // esto falto en el diagrama 
        return new InsigniaObtenida(insignia, true);
    }

    public abstract int progresoActual(Donante donante);

    public int restante(Donante donante) {
        return Math.max(0, this.objetivo - progresoActual(donante));
    }

    public abstract boolean estaCompletada(Donante donante);
}
