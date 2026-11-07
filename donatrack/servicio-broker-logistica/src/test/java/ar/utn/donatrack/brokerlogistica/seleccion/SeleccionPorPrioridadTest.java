package ar.utn.donatrack.brokerlogistica.seleccion;

import ar.utn.donatrack.brokerlogistica.adapters.ProveedorLogisticaAdapter;
import ar.utn.donatrack.brokerlogistica.exceptions.ProveedorDesconocidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SeleccionPorPrioridad - orden de candidatos")
class SeleccionPorPrioridadTest {

    private ProveedorLogisticaAdapter proveedor(String nombre) {
        ProveedorLogisticaAdapter adapter = mock(ProveedorLogisticaAdapter.class);
        when(adapter.nombre()).thenReturn(nombre);
        return adapter;
    }

    @Test
    @DisplayName("Sin proveedor pedido, ordena según la prioridad configurada y deja al final los no listados")
    void ordenaPorPrioridad() {
        ProveedorLogisticaAdapter externa = proveedor("EXTERNA");
        ProveedorLogisticaAdapter donatrack = proveedor("DONATRACK");
        ProveedorLogisticaAdapter otro = proveedor("OTRO");
        SeleccionPorPrioridad criterio = new SeleccionPorPrioridad(List.of("donatrack", " EXTERNA"));

        assertThat(criterio.candidatos(List.of(otro, externa, donatrack), null))
                .containsExactly(donatrack, externa, otro);
    }

    @Test
    @DisplayName("Con proveedor pedido, devuelve solo ese (sin importar mayúsculas)")
    void respetaProveedorPedido() {
        ProveedorLogisticaAdapter externa = proveedor("EXTERNA");
        ProveedorLogisticaAdapter donatrack = proveedor("DONATRACK");
        SeleccionPorPrioridad criterio = new SeleccionPorPrioridad(List.of("DONATRACK", "EXTERNA"));

        assertThat(criterio.candidatos(List.of(donatrack, externa), "externa")).containsExactly(externa);
    }

    @Test
    @DisplayName("Un proveedor pedido que no existe lanza ProveedorDesconocidoException")
    void proveedorDesconocido() {
        SeleccionPorPrioridad criterio = new SeleccionPorPrioridad(List.of("DONATRACK"));
        List<ProveedorLogisticaAdapter> proveedores = List.of(proveedor("DONATRACK"));

        assertThatThrownBy(() -> criterio.candidatos(proveedores, "CORREO"))
                .isInstanceOf(ProveedorDesconocidoException.class);
    }
}
