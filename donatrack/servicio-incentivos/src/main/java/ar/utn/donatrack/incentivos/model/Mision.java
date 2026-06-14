package ar.utn.donatrack.incentivos.model;

import java.util.UUID;

/**
 * Define una misión que el donante debe completar para obtener
 * una insignia y avanzar hacia la siguiente categoría.
 *
 * Las misiones se completan en orden (campo 'orden') dentro de cada categoría.
 */
public class Mision {

    private final UUID id;
    private final String nombre;
    private final String descripcion;
    private final TipoMision tipo;
    private final CategoriaDonante categoriaRequerida; // a qué categoría pertenece
    private final int objetivo;   // cantidad a alcanzar (ej: 3 meses, 5 categorías)
    private final int orden;      // orden dentro de la categoría (1, 2, 3...)

    public Mision(String nombre, String descripcion, TipoMision tipo,
                  CategoriaDonante categoriaRequerida, int objetivo, int orden) {
        this.id                 = UUID.randomUUID();
        this.nombre             = nombre;
        this.descripcion        = descripcion;
        this.tipo               = tipo;
        this.categoriaRequerida = categoriaRequerida;
        this.objetivo           = objetivo;
        this.orden              = orden;
    }

    public UUID getId()                              { return id; }
    public String getNombre()                        { return nombre; }
    public String getDescripcion()                   { return descripcion; }
    public TipoMision getTipo()                      { return tipo; }
    public CategoriaDonante getCategoriaRequerida()  { return categoriaRequerida; }
    public int getObjetivo()                         { return objetivo; }
    public int getOrden()                            { return orden; }
}
