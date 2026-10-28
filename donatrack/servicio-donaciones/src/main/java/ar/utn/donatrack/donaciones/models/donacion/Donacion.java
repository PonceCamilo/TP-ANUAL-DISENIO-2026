package ar.utn.donatrack.donaciones.models.donacion;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostPersist;
import ar.utn.donatrack.donaciones.models.EntidadPersistible;
import ar.utn.donatrack.donaciones.exceptions.cambioEstadosExceptions.TransicionReservadaException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.BienIncompatibleConDonacionException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.BienNoEncontradoException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionConVariosBienesException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionNoModificableException;
import ar.utn.donatrack.donaciones.exceptions.donacionesExceptions.DonacionSinBienesException;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.BatchSize;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ASIGNACION_REALIZADA;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.ENTREGADA;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.EN_DEPOSITO;
import static ar.utn.donatrack.donaciones.models.donacion.estado.EstadosDonacion.LISTA_PARA_ENTREGAR;

/**
 * Una donación ya segmentada: pertenece a una única subcategoría y es la unidad
 * mínima de asignación del sistema.
 *
 * MAPEO DEL PATRÓN STATE (Entrega 4) — el punto más interesante del mapeo:
 *
 * el estado es un objeto de la jerarquía EstadoDonacionBase, no un enum, y NO se
 * persiste. Lo único que se guarda es su nombre, en la columna `estado`, y
 * getEstado() arma el objeto al vuelo con EstadosDonacion, igual que
 * CambioEstado. Antes había dos campos (el nombre y el objeto) que había que
 * mantener sincronizados con un @PostLoad; con uno solo no pueden desfasarse.
 *
 * Por qué así y no con un enum: el estado no es un dato, es comportamiento —cada
 * clase conoce sus transiciones válidas. Convertirlo en enum para que JPA lo
 * mapeara directo habría significado mover esas reglas a un switch, que es
 * exactamente lo que el patrón evita. Guardar el nombre cuesta una línea de
 * rehidratación y deja la columna legible en el DER.
 */
@Entity
@Table(name = "donacion", indexes = {
        // Filtros de GET /donaciones y de los procesos nocturnos (por estado).
        @Index(name = "idx_donacion_estado", columnList = "estado"),
        // "Mis donaciones": un donante filtra las suyas, casi siempre por estado.
        @Index(name = "idx_donacion_donante_estado", columnList = "id_donante, estado"),
        // Donaciones de una entidad y reconciliación del contador de recepciones.
        @Index(name = "idx_donacion_entidad_fecha", columnList = "id_entidad_beneficiaria, fecha_asignacion"),
        @Index(name = "idx_donacion_subcategoria", columnList = "subcategoria_tipo_normalizado"),
        // Orden de la paginación: más nuevas primero, el id desempata.
        @Index(name = "idx_donacion_fecha", columnList = "fecha_donacion DESC, id")
})
@Getter
public class Donacion extends EntidadPersistible {

    /**
     * La donación es dueña de sus bienes: se guardan y se borran con ella
     * (cascade + orphanRemoval). No tienen sentido por fuera de una donación.
     */
    @Setter
    // @BatchSize (Entrega 5): al leer una página de donaciones, Hibernate trae
    // los bienes de hasta 100 donaciones en una sola consulta, en lugar de una
    // consulta por donación. Lo mismo vale para el historial.
    @BatchSize(size = 100)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "donacion_id")
    protected List<Bien> bienes = new ArrayList<>();

    @Setter
    @Embedded
    protected Subcategoria subcategoria;

    /** Nombre del estado actual: el único dato del estado que llega a la base. */
    @Column(name = "estado", nullable = false)
    protected String estadoNombre = EstadosDonacion.inicial().nombre();

    /** Historial ordenado por fecha: es la trazabilidad que exige el enunciado. */
    @BatchSize(size = 100)
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
     * qué identificador la sigue él (el id de la entrega en DONATRACK, un trackingId
     * como EXT-7838BF20 en EXTERNA). Los asigna el broker al despachar el envío.
     */
    @Setter
    @Column(name = "proveedor_logistica")
    protected String proveedorLogistica;

    @Setter
    @Column(name = "id_seguimiento_logistica")
    protected String idSeguimientoLogistica;

    /**
     * La asignación que ya está en la base: la que se leyó o la del último
     * guardado. Con ella el repositorio sabe qué mover en el contador de
     * recepciones cuando la asignación cambia, o cuando la donación se borra, sin
     * volver a consultar la base.
     */
    @Transient
    @Getter(AccessLevel.NONE)
    private Asignacion asignacionGuardada = Asignacion.NINGUNA;

    public Asignacion asignacion() {
        return new Asignacion(idEntidadBeneficiaria, fechaAsignacion);
    }

    public Asignacion asignacionGuardada() {
        return asignacionGuardada;
    }

    /** Si la entidad o la fecha de la asignación cambiaron desde la última vez que se guardó. */
    public boolean cambioLaAsignacion() {
        return !asignacion().equals(asignacionGuardada);
    }

    /** Lo llama el repositorio al guardar, y JPA al leer, insertar o actualizar la fila. */
    @PostLoad
    @PostPersist
    @PostUpdate
    public void marcarAsignacionGuardada() {
        this.asignacionGuardada = asignacion();
    }

    /** El objeto de estado, armado a partir del nombre guardado. */
    public EstadoDonacionBase getEstado() {
        return EstadosDonacion.desde(estadoNombre);
    }

    public void cambiarEstado(String estadoDestino, String nombreTransicion, String justificacion) {
        EstadoDonacionBase previo = getEstado();
        EstadoDonacionBase nuevo = previo.transicionarA(estadoDestino, justificacion);
        this.estadoNombre = nuevo.nombre();
        historialEstados.add(CambioEstado.de(previo, nuevo, nombreTransicion, justificacion));
    }

    /**
     * Reemplaza un bien por su versión corregida.
     *
     * Solo mientras la donación está EN_DEPOSITO, y sin romper la segmentación:
     * el bien sigue siendo de la subcategoría de la donación y, si la donación
     * tiene otros bienes, comparte con ellos la clave de segmentación (mismo tipo
     * y mismo vencimiento o mismo estado). Con un solo bien, la clave puede
     * cambiar, porque es la de toda la donación.
     *
     * El bien nuevo tiene su propio id: se reemplaza el objeto, no se edita.
     */
    public void reemplazarBien(UUID idBien, Bien nuevo) {
        if (!estaEnEstado(EN_DEPOSITO)) {
            throw new DonacionNoModificableException(id, estadoNombre);
        }
        int posicion = posicionDelBien(idBien);
        if (subcategoria != null && !subcategoria.equals(nuevo.getSubcategoria())) {
            throw new BienIncompatibleConDonacionException(
                    "la donación es de la subcategoría '" + subcategoria + "' y el bien de '" + nuevo.getSubcategoria() + "'.");
        }
        boolean rompeLaSegmentacion = bienes.stream()
                .filter(bien -> !bien.getId().equals(idBien))
                .anyMatch(otro -> !otro.claveDeSegmentacion().equals(nuevo.claveDeSegmentacion()));
        if (rompeLaSegmentacion) {
            throw new BienIncompatibleConDonacionException(
                    "los demás bienes de la donación tienen otro tipo, vencimiento o estado.");
        }
        bienes.set(posicion, nuevo);
    }

    /** Para donaciones de un solo bien, que es lo que suele producir la segmentación. */
    public void reemplazarUnicoBien(Bien nuevo) {
        if (bienes.isEmpty()) {
            throw new DonacionSinBienesException(id);
        }
        if (bienes.size() > 1) {
            throw new DonacionConVariosBienesException(id, bienes.size());
        }
        reemplazarBien(bienes.getFirst().getId(), nuevo);
    }

    private int posicionDelBien(UUID idBien) {
        for (int i = 0; i < bienes.size(); i++) {
            if (bienes.get(i).getId().equals(idBien)) {
                return i;
            }
        }
        throw new BienNoEncontradoException(id, idBien);
    }

    /**
     * Si algún bien perecedero ya venció a la fecha indicada. Una donación así no
     * entra al matching ni se asigna; la persona administradora la pasa a VENCIDA.
     */
    public boolean tieneBienesVencidosAl(LocalDate fecha) {
        return bienes.stream()
                .map(Bien::vencimiento)
                .flatMap(Optional::stream)
                .anyMatch(vencimiento -> vencimiento.isBefore(fecha));
    }

    /**
     * Cambio de estado pedido por la persona administradora (PATCH .../estado).
     *
     * ASIGNACION_REALIZADA queda afuera: a ese estado se llega asignando la
     * donación, que además registra la entidad, la fecha y el contador de
     * recepciones. Antes se podía pasar a mano y la donación quedaba asignada a
     * nadie. Las demás transiciones manuales siguen permitidas como intervención
     * de la persona administradora.
     *
     * El nombre de la transición se deriva del estado de origen y el de destino
     * ("Cambio manual de EN_DEPOSITO a VENCIDA"): antes lo mandaba el cliente como
     * texto libre y el historial podía decir cualquier cosa.
     */
    public void cambiarEstadoManualmente(String estadoDestino, String justificacion) {
        if (ASIGNACION_REALIZADA.equals(estadoDestino)) {
            throw new TransicionReservadaException(estadoDestino,
                    "se llega asignando la donación (PATCH /donaciones/" + id + "/asignar).");
        }
        cambiarEstado(estadoDestino, "Cambio manual de " + estadoNombre + " a " + estadoDestino, justificacion);
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
        cambiarEstado(LISTA_PARA_ENTREGAR, "envio a logistica",
                "Tomada por " + proveedor + " (seguimiento " + idSeguimiento + ")");
    }

    public void asignarA(EntidadBeneficiaria entidad) {
        this.idEntidadBeneficiaria = entidad.getId();
        this.fechaAsignacion = FechaHoraArgentina.hoy();
        cambiarEstado(ASIGNACION_REALIZADA, "asignar", "Asignada a " + entidad.getRazonSocial());
    }

    public boolean estaEnEstado(String nombreEstado) {
        return estadoNombre.equals(nombreEstado);
    }

    public boolean esDeSubcategoria(String subcategoria) {
        return this.subcategoria != null
                && this.subcategoria.getTipo() != null
                && this.subcategoria.getTipo().equalsIgnoreCase(subcategoria);
    }

    public boolean fueEntregada() {
        return estaEnEstado(ENTREGADA);
    }
}
