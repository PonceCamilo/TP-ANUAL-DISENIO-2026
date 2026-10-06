package ar.utn.donatrack.brokerlogistica.seleccion;

import ar.utn.donatrack.brokerlogistica.adapters.ProveedorLogisticaAdapter;
import ar.utn.donatrack.brokerlogistica.exceptions.ProveedorDesconocidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Criterio por defecto:
 *  - si Donaciones pide un proveedor puntual, se usa solo ese (sin fallback:
 *    se respeta la elección);
 *  - si no, todos los proveedores ordenados según broker.proveedores.prioridad,
 *    para que el broker haga fallback al siguiente si uno no está disponible.
 */
@Component
public class SeleccionPorPrioridad implements CriterioSeleccionProveedor {

    private final List<String> prioridad;

    public SeleccionPorPrioridad(@Value("${broker.proveedores.prioridad}") List<String> prioridad) {
        this.prioridad = prioridad.stream().map(String::trim).map(String::toUpperCase).toList();
    }

    @Override
    public List<ProveedorLogisticaAdapter> candidatos(List<ProveedorLogisticaAdapter> proveedores, String proveedorSolicitado) {
        if (proveedorSolicitado != null && !proveedorSolicitado.isBlank()) {
            return proveedores.stream()
                    .filter(p -> p.nombre().equalsIgnoreCase(proveedorSolicitado.trim()))
                    .findFirst()
                    .map(List::of)
                    .orElseThrow(() -> new ProveedorDesconocidoException(
                            proveedorSolicitado, proveedores.stream().map(ProveedorLogisticaAdapter::nombre).toList()));
        }
        return proveedores.stream()
                .sorted(Comparator.comparingInt(p -> posicion(p.nombre())))
                .toList();
    }

    /** Los que no figuran en la prioridad van al final. */
    private int posicion(String nombre) {
        int indice = prioridad.indexOf(nombre.toUpperCase());
        return indice >= 0 ? indice : Integer.MAX_VALUE;
    }
}
