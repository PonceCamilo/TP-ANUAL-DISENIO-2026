package ar.utn.donatrack.logistica.validations;

import ar.utn.donatrack.logistica.exceptions.TransicionEntregaIlegalException;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntregaValidatorTest {

    private final EntregaValidator validador = new EntregaValidator();

    @Test
    @DisplayName("PENDIENTE -> EN_TRASLADO es válida (inicio de ruta)")
    void pendienteAEnTrasladoEsValida() {
        assertDoesNotThrow(() -> validador.validarTransicion(EstadoEntrega.PENDIENTE, EstadoEntrega.EN_TRASLADO));
    }

    @Test
    @DisplayName("EN_TRASLADO -> ENTREGADA es válida (confirmación de la entidad)")
    void enTrasladoAEntregadaEsValida() {
        assertDoesNotThrow(() -> validador.validarTransicion(EstadoEntrega.EN_TRASLADO, EstadoEntrega.ENTREGADA));
    }

    @Test
    @DisplayName("EN_TRASLADO -> NO_RECIBIDA es válida")
    void enTrasladoANoRecibidaEsValida() {
        assertDoesNotThrow(() -> validador.validarTransicion(EstadoEntrega.EN_TRASLADO, EstadoEntrega.NO_RECIBIDA));
    }

    @Test
    @DisplayName("NO_RECIBIDA -> PENDIENTE es válida (regreso a depósito)")
    void noRecibidaAPendienteEsValida() {
        assertDoesNotThrow(() -> validador.validarTransicion(EstadoEntrega.NO_RECIBIDA, EstadoEntrega.PENDIENTE));
    }

    @Test
    @DisplayName("ENTREGADA es un estado terminal: cualquier transición es inválida")
    void entregadaEsTerminal() {
        assertThrows(TransicionEntregaIlegalException.class,
                () -> validador.validarTransicion(EstadoEntrega.ENTREGADA, EstadoEntrega.PENDIENTE));
    }

    @Test
    @DisplayName("PENDIENTE -> ENTREGADA es inválida: no se puede saltear EN_TRASLADO")
    void pendienteAEntregadaEsInvalida() {
        assertThrows(TransicionEntregaIlegalException.class,
                () -> validador.validarTransicion(EstadoEntrega.PENDIENTE, EstadoEntrega.ENTREGADA));
    }

    @Test
    @DisplayName("EN_TRASLADO -> PENDIENTE es inválida")
    void enTrasladoAPendienteEsInvalida() {
        assertThrows(TransicionEntregaIlegalException.class,
                () -> validador.validarTransicion(EstadoEntrega.EN_TRASLADO, EstadoEntrega.PENDIENTE));
    }

    @Test
    @DisplayName("NO_RECIBIDA -> ENTREGADA es inválida")
    void noRecibidaAEntregadaEsInvalida() {
        assertThrows(TransicionEntregaIlegalException.class,
                () -> validador.validarTransicion(EstadoEntrega.NO_RECIBIDA, EstadoEntrega.ENTREGADA));
    }
}
