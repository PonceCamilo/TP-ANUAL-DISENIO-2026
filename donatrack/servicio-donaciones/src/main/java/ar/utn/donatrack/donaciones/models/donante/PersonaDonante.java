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
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Raíz de la jerarquía de personas donantes: humanas u organizaciones.
 *
 * El email es un campo directo y no un elemento de `contactos` porque es la
 * clave de idempotencia de la importación masiva por CSV y de las búsquedas.
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
@Table(name = "persona_donante")
@Inheritance(strategy = InheritanceType.JOINED)
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public abstract class PersonaDonante {

    @Id
    @Column(name = "id")
    protected UUID id;

    @Column(name = "tipo_documento")
    protected String tipoDocumento;

    @Column(name = "numero_documento")
    protected String numeroDocumento;

    @Embedded
    protected Direccion direccion;

    /** Nombre del estado: ACTIVO, INACTIVO o BLOQUEADO. */
    @Column(name = "estado", nullable = false)
    protected String estadoNombre = EstadosDonante.inicial().nombre();

    /** El objeto de estado, con sus transiciones válidas. No se persiste. */
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

    @Column(name = "email")
    protected String email;

    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_donante_id")
    protected List<MedioDeContacto> contactos = new ArrayList<>();

    /** Reconstruye el objeto de estado después de cargar la fila. */
    @PostLoad
    private void rehidratarEstado() {
        this.estado = EstadosDonante.desde(this.estadoNombre);
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
