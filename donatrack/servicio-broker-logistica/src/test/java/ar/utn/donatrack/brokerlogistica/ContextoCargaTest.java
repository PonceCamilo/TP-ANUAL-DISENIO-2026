package ar.utn.donatrack.brokerlogistica;

import ar.utn.donatrack.brokerlogistica.adapters.ProveedorLogisticaAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el contexto de Spring cargue y que los dos proveedores de
 * logística queden registrados.
 *
 * Lo segundo importa especialmente: el broker existe para elegir entre varios
 * proveedores. Si uno de los adapters no se registrara como bean, el broker
 * seguiría funcionando con el otro y nadie notaría que perdió el fallback —
 * justo la capacidad que la Entrega 4 pide demostrar.
 *
 * Este servicio no tiene base de datos: es un router, y su registro de envíos
 * vive en memoria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Carga del contexto y proveedores registrados")
class ContextoCargaTest {

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private List<ProveedorLogisticaAdapter> proveedores;

    @Test
    @DisplayName("El contexto levanta sin errores")
    void elContextoCarga() {
        assertThat(contexto).isNotNull();
    }

    @Test
    @DisplayName("Los dos proveedores quedan registrados, así el fallback es posible")
    void losDosProveedoresEstanRegistrados() {
        assertThat(proveedores)
                .as("Con un solo proveedor el broker no puede hacer fallback")
                .hasSizeGreaterThanOrEqualTo(2);
    }
}
