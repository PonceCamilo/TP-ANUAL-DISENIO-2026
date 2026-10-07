package ar.utn.donatrack.brokerlogistica.seleccion;

import ar.utn.donatrack.brokerlogistica.adapters.ProveedorLogisticaAdapter;

import java.util.List;

/**
 * Strategy: decide a qué proveedores (y en qué orden) se le ofrece un pedido.
 * El broker los prueba en ese orden hasta que uno lo acepte.
 */
public interface CriterioSeleccionProveedor {

    /**
     * @param proveedorSolicitado nombre pedido explícitamente por Donaciones, o null.
     */
    List<ProveedorLogisticaAdapter> candidatos(List<ProveedorLogisticaAdapter> proveedores, String proveedorSolicitado);
}
