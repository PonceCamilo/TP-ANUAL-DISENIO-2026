package ar.utn.donatrack.donaciones.model.entidad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ar.utn.donatrack.donaciones.model.entidad.NecesidadExtraordinaria;

class NecesidadExtraordinariaTest {

    private NecesidadExtraordinaria necesidad;

    @BeforeEach
    void setUp() {
        necesidad = new NecesidadExtraordinaria();
        necesidad.setCantidadRequerida(30); 
        necesidad.setCantidadRecibida(0);
    }

    @Test
    void estaSatisfecha_DeberiaRetornarFalse_CuandoNoSeAlcanzaLaCantidadRequerida() {
        // ej: Una persona dona 2
        necesidad.recibirDonacion(2);
        
        assertFalse(necesidad.estaSatisfecha(), 
            "La necesidad no debería estar satisfecha si no se alcanzó el total.");
    }

    @Test
    void estaSatisfecha_DeberiaRetornarTrue_CuandoSeIgualaLaCantidadRequerida() {
        // ej: donaciones parciales hasta llegar a 30
        necesidad.recibirDonacion(10);
        necesidad.recibirDonacion(20);
        
        assertTrue(necesidad.estaSatisfecha(), 
            "La necesidad debería estar satisfecha al igualar la cantidad requerida.");
    }

    @Test
    void estaSatisfecha_DeberiaRetornarTrue_CuandoSeSuperaLaCantidadRequerida() {
        // Se reciben más de los 30 solicitados
        necesidad.recibirDonacion(35);
        
        assertTrue(necesidad.estaSatisfecha(), 
            "La necesidad debería estar satisfecha si se supera la cantidad requerida.");
    }
}
