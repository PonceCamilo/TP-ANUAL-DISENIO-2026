package ar.utn.donatrack.donaciones.model.entidad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.utn.donatrack.donaciones.model.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.model.entidad.NecesidadExtraordinaria;

class EntidadBeneficiariaTest {

    private EntidadBeneficiaria entidad;
    private NecesidadExtraordinaria necesidad;

    @BeforeEach
    void setUp() {
        entidad = new EntidadBeneficiaria();
        necesidad = new NecesidadExtraordinaria();
    }

    @Test
    void registrarNecesidad_agregaUnaNecesidadALaLista() {

        entidad.registrarNecesidad(necesidad);

        assertEquals(1, entidad.getNecesidades().size());
        assertTrue(entidad.getNecesidades().contains(necesidad));
    }
}