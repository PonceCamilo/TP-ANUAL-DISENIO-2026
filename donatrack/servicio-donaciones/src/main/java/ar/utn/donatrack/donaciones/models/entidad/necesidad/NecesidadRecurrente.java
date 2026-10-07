package ar.utn.donatrack.donaciones.models.entidad.necesidad;

import ar.utn.donatrack.donaciones.models.entidad.necesidad.periodicidades.Periodicidad;
import ar.utn.donatrack.donaciones.util.FechaHoraArgentina;
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
 * Al vencer el período, la cantidad recibida se reinicia para el siguiente.
 *
 * Campo clave: fechaInicioPeriodo marca el inicio del período vigente. Se usa en
 * lugar de fechaRegistro para poder reiniciarla sin perder el dato original.
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

    /** Inicio del período actual. Se actualiza cada vez que arranca uno nuevo. */
    @Column(name = "fecha_inicio_periodo")
    private LocalDate fechaInicioPeriodo;

    /**
     * Determina si el período vigente ya venció respecto a una fecha de referencia.
     * Recibe la fecha como parámetro para no depender de LocalDate.now() y poder
     * testearlo con fechas fijas.
     */
    public boolean periodoVencido(LocalDate referencia) {
        if (fechaInicioPeriodo == null) return false;
        LocalDate fechaVencimiento = fechaInicioPeriodo.plusDays(periodo.getDias());
        return referencia.isAfter(fechaVencimiento);
    }

    /**
     * Si el período venció respecto a `hoy`, reinicia el contador de lo recibido
     * y arranca un período nuevo. Si sigue vigente, no hace nada.
     */
    public void obtenerOGenerarPeriodoActual(LocalDate hoy) {
        if (periodoVencido(hoy)) {
            this.cantidadRecibida = 0;
            this.fechaInicioPeriodo = hoy;
        }
    }

    /**
     * Está satisfecha solo si alcanzó el objetivo DENTRO del período vigente.
     * Es la diferencia central con la extraordinaria: aunque el contador diga que
     * se alcanzó el objetivo, si el período venció la necesidad vuelve a estar abierta.
     */
    public boolean estaSatisfecha() {
        return cantidadRecibida >= cantidadObjetivo && !periodoVencido(FechaHoraArgentina.hoy());
    }
}
