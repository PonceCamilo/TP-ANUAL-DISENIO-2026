package ar.utn.donatrack.donaciones.model.entidad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ar.utn.donatrack.donaciones.model.entidad.NecesidadRecurrente;

class NecesidadRecurrenteTest {

    private NecesidadRecurrente necesidad;

    @BeforeEach
    void setUp() {
        necesidad = new NecesidadRecurrente();
        necesidad.setCantidadObjetivoPorPeriodo(100);
        necesidad.setPeriodoEnDias(7); 
        necesidad.setCantidadRecibidaEnPeriodo(0);
    }

    @Test
    void estaSatisfecha_DeberiaRetornarFalse_CuandoNoSeAlcanzaElObjetivoDelPeriodo() {
        necesidad.setCantidadRecibidaEnPeriodo(45);
        
        assertFalse(necesidad.estaSatisfecha(), 
            "No debería estar satisfecha si no se llegó al objetivo semanal.");
    }

    @Test
    void estaSatisfecha_DeberiaRetornarTrue_CuandoSeIgualaOSuperaElObjetivoDelPeriodo() {
        necesidad.setCantidadRecibidaEnPeriodo(100);
        assertTrue(necesidad.estaSatisfecha(), "Debería estar satisfecha al alcanzar los 100 paquetes.");

        necesidad.setCantidadRecibidaEnPeriodo(120);
        assertTrue(necesidad.estaSatisfecha(), "Debería seguir satisfecha si se supera el objetivo.");
    }
}
