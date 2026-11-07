package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Necesidad vinculada al funcionamiento habitual de la organización.
 * Se satisface dentro de cada período (ej: 100 paquetes de fideos por semana).
 * Al vencer el período, lo recibido deja de contar y arranca el siguiente.
 *
 * Campo clave: fechaInicioPeriodo marca el inicio del período vigente. Se usa en
 * lugar de fechaRegistro para poder avanzarla sin perder el dato original.
 *
 * El período es [inicio, inicio + duración): una necesidad semanal que arrancó
 * un lunes cubre hasta el domingo, y el lunes siguiente empieza otro.
 */
@Entity
@Table(name = "necesidad_recurrente")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor
@Getter
@Setter
public class NecesidadRecurrente extends Necesidad {

    @Enumerated(EnumType.STRING)
    @Column(name = "periodo")
    private Periodicidad periodo;

    /** Inicio del período actual. Avanza cada vez que arranca uno nuevo. */
    @Column(name = "fecha_inicio_periodo")
    private LocalDate fechaInicioPeriodo;

    @Override
    public TipoNecesidad tipo() {
        return TipoNecesidad.RECURRENTE;
    }

    /**
     * Si el período vigente ya terminó a la fecha de referencia. Recibe la fecha
     * como parámetro para poder probarlo con fechas fijas.
     */
    public boolean periodoVencido(LocalDate referencia) {
        return fechaInicioPeriodo != null && !referencia.isBefore(periodo.finDelPeriodo(fechaInicioPeriodo));
    }

    /**
     * Si el período venció, pasa al que corresponde a la fecha y vuelve lo
     * recibido a cero. Avanza de a períodos enteros desde el inicio anterior, así
     * una necesidad semanal sigue empezando el mismo día de la semana.
     */
    public void actualizarPeriodo(LocalDate hoy) {
        if (periodoVencido(hoy)) {
            this.fechaInicioPeriodo = periodo.inicioDelPeriodoVigente(fechaInicioPeriodo, hoy);
            this.cantidadRecibida = 0;
        }
    }

    @Override
    public void recibir(int cantidad, LocalDate fecha) {
        actualizarPeriodo(fecha);
        super.recibir(cantidad, fecha);
    }

    /**
     * Lo recibido de un período vencido no cuenta para el siguiente: la
     * necesidad vuelve a estar abierta aunque el contador diga que llegó al
     * objetivo. Es la diferencia central con la extraordinaria.
     */
    @Override
    public int cantidadRecibidaAl(LocalDate fecha) {
        return periodoVencido(fecha) ? 0 : cantidadRecibida;
    }
}
