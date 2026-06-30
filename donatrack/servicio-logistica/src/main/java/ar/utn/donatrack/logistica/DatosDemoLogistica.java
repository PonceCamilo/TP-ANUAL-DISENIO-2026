package ar.utn.donatrack.logistica;

import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Carga una entrega de ejemplo (en estado EN_TRASLADO) al arrancar el servicio,
 * para poder probar a mano el flujo de "entrega no recibida" sin tener que pasar
 * por toda la planificacion con el proveedor externo de ruteo.
 *
 * Es solo para pruebas locales: los repositorios son en memoria, asi que el dato
 * se pierde al reiniciar y se vuelve a crear en el siguiente arranque.
 */
@Component
public class DatosDemoLogistica implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoLogistica.class);

    public static final UUID ENTREGA_DEMO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final EntregaRepositoryInterface entregaRepositorio;

    public DatosDemoLogistica(EntregaRepositoryInterface entregaRepositorio) {
        this.entregaRepositorio = entregaRepositorio;
    }

    @Override
    public void run(String... args) {
        Entrega entrega = Entrega.builder()
                .id(ENTREGA_DEMO_ID)
                .idDonacion(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .idEntidadBeneficiaria(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .idDonante(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .rutaId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                .camionId(UUID.fromString("66666666-6666-6666-6666-666666666666"))
                .estado(EstadoEntrega.EN_TRASLADO)
                .build();
        entregaRepositorio.guardar(entrega);

        log.info("[DatosDemoLogistica] Entrega de prueba cargada: {} (estado EN_TRASLADO)", ENTREGA_DEMO_ID);
    }
}
