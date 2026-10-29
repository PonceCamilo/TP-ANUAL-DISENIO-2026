package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;

import java.util.List;

/**
 * Contrato para los algoritmos de asignación de donaciones (patrón Strategy).
 * Cada algoritmo evalúa una donación contra un conjunto de entidades
 * beneficiarias y devuelve un ranking ordenado de mejor a peor puntaje.
 *
 * Los algoritmos se enchufan: AsignacionDonacionesService ejecuta todos los
 * beans que implementan esta interfaz, y el ranking de cada uno se guarda y se
 * devuelve con su nombre. Sumar un algoritmo (por ejemplo, cercanía geográfica)
 * es una clase @Component nueva, sin tocar el orquestador, las tablas ni los
 * DTOs, tal como anticipa el enunciado.
 */
public interface AlgoritmoAsignacion {

    /** Nombre estable del algoritmo: identifica su ranking en la base y en el JSON. */
    String nombre();

    /** Hasta `tamanio` entidades candidatas, de mejor a peor puntaje. */
    List<ResultadoAsignacion> evaluar(Donacion donacion, List<EntidadBeneficiaria> entidades, int tamanio);
}
