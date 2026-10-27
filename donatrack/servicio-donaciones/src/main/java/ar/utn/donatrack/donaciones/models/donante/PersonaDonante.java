package ar.utn.donatrack.donaciones.models.donante;

import ar.utn.donatrack.donaciones.models.contacto.MedioDeContacto;
import ar.utn.donatrack.donaciones.models.donante.estado.EstadoDonante;
import ar.utn.donatrack.donaciones.models.donante.estado.EstadosDonante;
import ar.utn.donatrack.donaciones.models.entidad.Direccion;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import ar.utn.donatrack.donaciones.util.NormalizadorEmail;
import org.hibernate.annotations.BatchSize;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Persistable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Raíz de la jerarquía de personas donantes: humanas u organizaciones.
 *
 * El email es un campo directo y no un elemento de `contactos` porque es la
 * clave de idempotencia de la importación masiva por CSV y de las búsquedas.
 * Es la identidad de la persona: se fija en el alta y no se cambia después.
 * Los contactos de tipo EMAIL que se agreguen más tarde son canales adicionales,
 * no reemplazan a esta identidad.
 *
 * UNICIDAD (Entrega 5): la columna `email` tiene un UNIQUE en la base y se
 * guarda siempre en su forma canónica (ver NormalizadorEmail). Antes la unicidad
 * dependía solo de una consulta previa del servicio; dos altas simultáneas o una
 * diferencia de mayúsculas podían dejar duplicados.
 *
 * MAPEO DE LA HERENCIA (Entrega 4): JOINED.
 *
 * Al contrario que Bien o MedioDeContacto, acá las subclases sí tienen peso
 * propio: PersonaHumana agrega nombre, apellido, fecha de nacimiento y género;
 * PersonaJuridica agrega razón social, tipo, rubro y una colección de
 * representantes. Con SINGLE_TABLE la tabla quedaría con ocho columnas nullable
 * y sin forma de exigir en la base que una humana tenga nombre. JOINED cuesta
 * un JOIN y a cambio deja un modelo de datos normalizado, que es lo que se
 * evalúa en el DER.
 *
 * El estado se mapea igual que en Donacion: se guarda el nombre y el objeto se
 * rehidrata en @PostLoad.
 */
@Entity
@Table(name = "persona_donante", indexes = {
        @Index(name = "idx_persona_estado", columnList = "estado"),
        // Proceso nocturno de inactividad: busca por fecha de última interacción.
        @Index(name = "idx_persona_ultima_interaccion", columnList = "ultima_interaccion")
})
@Inheritance(strategy = InheritanceType.JOINED)
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public abstract class PersonaDonante implements Persistable<UUID> {

    @Id
    @Column(name = "id")
    protected UUID id;

    @Column(name = "tipo_documento")
    protected String tipoDocumento;

    @Column(name = "numero_documento")
    protected String numeroDocumento;

    @Embedded
    protected Direccion direccion;

    /**
     * Nombre del estado: ACTIVO, INACTIVO o BLOQUEADO.
     *
     * @Builder.Default hace falta porque la clase usa @SuperBuilder: sin él, una
     * persona construida con builder() quedaba con el estado en null y fallaba
     * al guardarse (la columna es NOT NULL).
     */
    @Builder.Default
    @Column(name = "estado", nullable = false)
    protected String estadoNombre = EstadosDonante.inicial().nombre();

    /** El objeto de estado, con sus transiciones válidas. No se persiste. */
    @Builder.Default
    @Transient
    protected EstadoDonante estado = EstadosDonante.inicial();

    /**
     * Canal elegido por la persona para recibir notificaciones. Es una instancia
     * aparte de las de `contactos`, no una referencia a una de ellas.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "medio_predeterminado_id")
    protected MedioDeContacto medioContactoPredeterminado;

    @Column(name = "ultima_interaccion")
    protected LocalDateTime ultimaInteraccion;

    /**
     * Cuándo se le avisó por última vez que estaba inactiva. Evita repetir el
     * aviso todas las noches: se vuelve a avisar recién cuando pasa otro período
     * completo de inactividad.
     */
    @Column(name = "ultimo_aviso_inactividad")
    protected LocalDateTime ultimoAvisoInactividad;

    @Column(name = "email", unique = true)
    protected String email;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_donante_id")
    protected List<MedioDeContacto> contactos = new ArrayList<>();

    /**
     * ESCRITURA (Entrega 5): el id se asigna antes de guardar, así que Spring
     * Data no puede deducir si la persona es nueva y hace un merge, que es un
     * SELECT previo a cada INSERT. Con este indicador las altas (sobre todo la
     * importación de miles de filas) van directo al INSERT. Una persona leída o
     * ya guardada deja de ser nueva y sus cambios se guardan con merge.
     */
    @Transient
    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected boolean nueva = true;

    @Override
    public boolean isNew() {
        return nueva;
    }

    /** Reconstruye el objeto de estado después de cargar la fila. */
    @PostLoad
    private void rehidratarEstado() {
        this.estado = EstadosDonante.desde(this.estadoNombre);
        this.nueva = false;
    }

    @PostPersist
    private void marcarPersistida() {
        this.nueva = false;
    }

    /**
     * Garantiza que a la base solo llegue la forma canónica del email, venga de
     * donde venga la persona (alta por la API, builder o importación CSV).
     */
    @PrePersist
    @PreUpdate
    private void normalizarEmail() {
        this.email = NormalizadorEmail.normalizar(this.email);
    }

    public void cambiarEstado(String estadoDestino, String justificacion) {
        this.estado = this.estado.transicionarA(estadoDestino, justificacion);
        this.estadoNombre = this.estado.nombre();
    }

    /**
     * Mantiene sincronizados el objeto de estado y el nombre que se persiste.
     * Sin esto, un setEstado() directo dejaría la columna con el valor viejo.
     */
    public void setEstado(EstadoDonante estado) {
        this.estado = estado;
        this.estadoNombre = estado == null ? null : estado.nombre();
    }

    /**
     * Reemplaza el contacto del MISMO tipo si ya existía: cargar un teléfono
     * nuevo actualiza el que había en lugar de dejar dos.
     *
     * Vive en el modelo y no en el repositorio porque es una regla de dominio.
     * Tenerla acá además evita el problema que apareció al pasar a JPA: si el
     * repositorio la aplicaba sobre una instancia propia, el guardado posterior
     * de la interacción —hecho sobre otra instancia— pisaba el cambio.
     */
    public void reemplazarContacto(MedioDeContacto medio) {
        Class<?> tipoNuevo = medio.getClass();
        contactos.removeIf(mc -> mc.getClass().equals(tipoNuevo));
        contactos.add(medio);
    }

    /** HUMANA o JURIDICA: lo responde cada subclase. */
    public abstract TipoPersona tipoPersona();

    /**
     * Aplica el nombre que trae una fila de la importación por CSV, donde nombre
     * y apellido (o la razón social) vienen en una sola columna.
     */
    public abstract void actualizarNombreImportado(String nombreORazonSocial);

    public boolean estaBloqueada() {
        return "BLOQUEADO".equals(estadoNombre);
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
