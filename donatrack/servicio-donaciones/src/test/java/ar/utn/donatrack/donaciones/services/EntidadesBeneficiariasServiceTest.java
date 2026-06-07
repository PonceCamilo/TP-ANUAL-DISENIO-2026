package ar.utn.donatrack.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;

import ar.utn.donatrack.donaciones.repositories.EntidadesBeneficiariasRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class EntidadesBeneficiariasServiceTest {

    private final EntidadesBeneficiariasRepository repository = new EntidadesBeneficiariasRepository();
    private final EntidadesBeneficiariasService service = new EntidadesBeneficiariasService(repository);

    private EntidadBeneficiaria comedor;
    private NecesidadExtraordinaria bancosSillas;
    private NecesidadRecurrente fideosSemanales;

    @BeforeEach
    void beforeEach() {
        this.repository.buscarTodas().clear();
        this.comedor = EntidadBeneficiaria.builder().build();
        comedor.setRazonSocial("Comedor Escobar Sonrisas");

        bancosSillas = new NecesidadExtraordinaria();
        bancosSillas.setDescripcion("Reposición por inundación");
        bancosSillas.setCantidadObjetivo(30);

        fideosSemanales = new NecesidadRecurrente();
        fideosSemanales.setDescripcion("Paquetes de fideos");
        fideosSemanales.setCantidadObjetivo(100);
        fideosSemanales.setPeriodo(Periodicidad.SEMANAL);
        
        this.repository.guardar(comedor);
    }

    @AfterEach
    void afterEach() {
        this.repository.buscarTodas().clear();
    }

    @Test
    void registrarNecesidad_agregaNecesidadALaEntidad() {
        service.registrarNecesidad(comedor, bancosSillas);
        assertEquals(1, comedor.getNecesidades().size());
        assertTrue(comedor.getNecesidades().contains(bancosSillas));
    }

    @Test
    void actualizarPeriodos_necesidadRecurrenteVencida_reiniciaCantidades() {
        service.registrarNecesidad(comedor, fideosSemanales);
        fideosSemanales.recibirDonacion(100);
        //Forzamos la fecha de inicio a hace 8 días (vencida para periodo SEMANAL)
        fideosSemanales.setFechaInicioPeriodo(LocalDate.now().minusDays(8));
        service.actualizarPeriodos();
        assertEquals(0, fideosSemanales.getCantidadRecibida(), "La cantidad debe volver a 0 porque inició una nueva semana");
        assertEquals(LocalDate.now(), fideosSemanales.getFechaInicioPeriodo(), "La fecha de inicio debe haberse actualizado a hoy");
    }

    @Test
    void actualizarPeriodos_necesidadRecurrenteEnFecha_noReiniciaCantidades() {
        service.registrarNecesidad(comedor, fideosSemanales);
        fideosSemanales.recibirDonacion(45);
        //Forzamos la fecha a hace 3 días (aún vigente dentro de la semana)
        fideosSemanales.setFechaInicioPeriodo(LocalDate.now().minusDays(3));
        service.actualizarPeriodos();
        assertEquals(45, fideosSemanales.getCantidadRecibida(), "No debe reiniciarse porque sigue en su periodo vigente");
        assertEquals(LocalDate.now().minusDays(3), fideosSemanales.getFechaInicioPeriodo());
    }

    @Test
    void actualizarPeriodos_necesidadExtraordinaria_noSeVeAfectada() {
        service.registrarNecesidad(comedor, bancosSillas);
        bancosSillas.recibirDonacion(20);
        service.actualizarPeriodos();
        assertEquals(20, bancosSillas.getCantidadRecibida(), "Las necesidades extraordinarias no tienen periodo, mantienen su acumulado");
    }
}


