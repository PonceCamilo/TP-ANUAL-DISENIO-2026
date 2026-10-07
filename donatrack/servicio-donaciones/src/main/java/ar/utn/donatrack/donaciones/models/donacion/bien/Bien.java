package ar.utn.donatrack.donaciones.models.donacion.bien;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Un artículo concreto dentro de una donación.
 *
 * MAPEO DE LA HERENCIA (Entrega 4): SINGLE_TABLE.
 *
 * Se eligió tabla única y no JOINED porque las tres subclases suman UN campo
 * propio entre todas (fechaVencimiento en el perecedero, esNuevo en el que
 * tiene estado, ninguno en el genérico). Con JOINED tendríamos tres tablas de
 * una columna cada una y un JOIN por cada lectura de donación, a cambio de
 * nada: el precio de SINGLE_TABLE son dos columnas nullable, que es mucho más
 * barato que el JOIN en una consulta que se hace todo el tiempo.
 *
 * El discriminador `tipo_bien` es el que permite que la segmentación siga
 * distinguiendo perecederos de bienes con estado al recuperarlos.
 */
@Entity
@Table(name = "bien")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_bien")
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public abstract class Bien {

    /**
     * Los bienes no tenían identidad propia en el modelo original: eran parte de
     * la donación. JPA necesita una clave primaria para cada entidad, así que se
     * agrega un id técnico que el dominio no usa.
     */
    @Id
    @Column(name = "id")
    @lombok.Builder.Default
    protected UUID id = UUID.randomUUID();

    @Embedded
    protected Subcategoria subcategoria;

    @Column(name = "descripcion")
    protected String descripcion;

    /** URL o path a la foto. */
    @Column(name = "foto")
    protected String foto;

    /** ej: 15 sillas → cantidad = 15, unidad = "unidades". */
    @Column(name = "cantidad")
    protected int cantidad;

    /** ej: "kg", "litros", "unidades". */
    @Column(name = "unidad")
    protected String unidad;
}
