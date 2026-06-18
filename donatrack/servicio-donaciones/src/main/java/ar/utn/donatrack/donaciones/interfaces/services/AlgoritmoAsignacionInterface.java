package ar.utn.donatrack.donaciones.interfaces.services;

import ar.utn.donatrack.donaciones.models.asignacion.ResultadoAsignacion;
import ar.utn.donatrack.donaciones.models.donacion.Donacion;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;

import java.util.List;

/**
 * Contrato para los algoritmos de asignación de donaciones (patrón Strategy).
 * Cada algoritmo evalúa una donación contra un conjunto de entidades
 * beneficiarias y devuelve un ranking de hasta 10 candidatas, ordenado
 * de mejor a peor puntaje.
 *
 * Permite incorporar nuevos algoritmos sin modificar el orquestador,
 * tal como anticipa el enunciado.
 */
public interface AlgoritmoAsignacionInterface {
    List<ResultadoAsignacion> evaluar(Donacion donacion, List<EntidadBeneficiaria> entidades);
}