package ar.utn.donatrack.donaciones.models.donacion;

import ar.utn.donatrack.donaciones.models.categoria.Subcategoria;
import ar.utn.donatrack.donaciones.models.donacion.bien.Bien;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienConEstado;
import ar.utn.donatrack.donaciones.models.donacion.bien.BienPerecible;
import ar.utn.donatrack.donaciones.models.donacion.estado.EstadoDonacionBase;
import ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Una donación ya segmentada: pertenece a una única subcategoría y es la unidad
 * mínima de asignación del sistema.
 *
 * MAPEO DEL PATRÓN STATE (Entrega 4) — el punto más interesante del mapeo:
 *
 * `estado` es un objeto de la jerarquía EstadoDonacionBase, no un enum, y NO se
 * persiste. Lo que se guarda es su nombre en la columna `estado`. Al cargar la
 * donación, @PostLoad vuelve a instanciar la clase correspondiente con
 * EstadosDonacion.desde(...).
 *
 * Por qué así y no con un enum: el estado no es un dato, es comportamiento —cada
 * clase conoce sus transiciones válidas. Convertirlo en enum para que JPA lo
 * mapeara directo habría significado mover esas reglas a un switch, que es
 * exactamente lo que el patrón evita. Guardar el nombre cuesta una línea de
 * rehidratación y deja la columna legible en el DER.
 */
@Entity
@Table(name = "donacion")
@Getter
public class Donacion {

    @Id
    @Setter
    @Column(name = "id")
    protected UUID id = UUID.randomUUID();

    /**
     * La donación es dueña de sus bienes: se guardan y se borran con ella
     * (cascade + orphanRemoval). No tienen sentido por fuera de una donación.
     */
    @Setter
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "donacion_id")
    protected List<Bien> bienes = new ArrayList<>();

    @Setter
    @Embedded
    protected Subcategoria subcategoria;

    /** Nombre del estado actual. Es la única forma en que el estado llega a la base. */
    @Column(name = "estado", nullable = false)
    protected String estadoNombre = EstadosDonacion.inicial().nombre();

    /** El objeto de estado: se rehidrata desde estadoNombre, nunca se persiste. */
    @Transient
    protected EstadoDonacionBase estado = EstadosDonacion.inicial();

    /** Historial ordenado por fecha: es la trazabilidad que exige el enunciado. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "donacion_id")
    @OrderBy("fechaHora ASC")
    protected List<CambioEstado> historialEstados = new ArrayList<>();

    @Setter
    @Column(name = "id_donante")
    protected UUID idDonante;

    @Setter
    @Column(name = "id_entidad_beneficiaria")
    protected UUID idEntidadBeneficiaria;

    @Setter
    @Column(name = "descripcion", length = 1000)
    protected String descripcion;

    @Setter
    @Column(name = "fecha_donacion")
    protected LocalDateTime fechaDonacion = FechaHoraArgentina.ahora();

    @Setter
    @Column(name = "fecha_asignacion")
    protected LocalDate fechaAsignacion;

    /**
     * Qué proveedor de logística tomó la donación ("DONATRACK" o "EXTERNA") y con
     * qué identificador la sigue él (el id del lote en DONATRACK, un trackingId
     * como EXT-7838BF20 en EXTERNA). Los asigna el broker al despachar el envío.
     */
    @Setter
    @Column(name = "proveedor_logistica")
    protected String proveedorLogistica;

    @Setter
    @Column(name = "id_seguimiento_logistica")
    protected String idSeguimientoLogistica;

    /** Reconstruye el objeto de estado después de cargar la fila. */
    @PostLoad
    private void rehidratarEstado() {
        this.estado = EstadosDonacion.desde(this.estadoNombre);
    }

    public void cambiarEstado(String estadoDestino, String nombreTransicion, String justificacion) {
        EstadoDonacionBase estadoPrevio = this.estado;
        this.estado = this.estado.transicionarA(estadoDestino, justificacion);
        this.estadoNombre = this.estado.nombre();
        historialEstados.add(CambioEstado.de(estadoPrevio, this.estado, nombreTransicion, justificacion));
    }

    public boolean esPerecible() {
        return !bienes.isEmpty() && bienes.getFirst() instanceof BienPerecible;
    }

    public boolean requiereEstado() {
        return !bienes.isEmpty() && bienes.getFirst() instanceof BienConEstado;
    }

    /**
     * Registra que un proveedor de logística se hizo cargo de la entrega y pasa la
     * donación a LISTA_PARA_ENTREGAR.
     *
     * El paso de estado es obligatorio: la máquina es
     * ASIGNACION_REALIZADA → LISTA_PARA_ENTREGAR → EN_TRASLADO. Si la donación
     * siguiera en ASIGNACION_REALIZADA cuando llega el evento de inicio de ruta,
     * procesarInicioRuta fallaría con una transición ilegal.
     */
    public void despacharA(String proveedor, String idSeguimiento) {
        this.proveedorLogistica = proveedor;
        this.idSeguimientoLogistica = idSeguimiento;
        cambiarEstado("LISTA_PARA_ENTREGAR", "envio a logistica",
                "Tomada por " + proveedor + " (seguimiento " + idSeguimiento + ")");
    }

    public void asignarA(EntidadBeneficiaria entidad) {
        this.idEntidadBeneficiaria = entidad.getId();
        this.fechaAsignacion = FechaHoraArgentina.hoy();
        cambiarEstado("ASIGNACION_REALIZADA", "asignar", "Asignada a " + entidad.getRazonSocial());
    }

    public boolean estaEnEstado(String nombreEstado) {
        return this.estado.nombre().equals(nombreEstado);
    }

    public boolean esDeSubcategoria(String subcategoria) {
        return this.subcategoria != null
                && this.subcategoria.getTipo() != null
                && this.subcategoria.getTipo().equalsIgnoreCase(subcategoria);
    }

    public boolean fueEntregada() {
        return estaEnEstado("ENTREGADA");
    }
}
