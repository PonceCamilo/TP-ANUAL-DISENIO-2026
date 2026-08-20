package ar.utn.donatrack.logistica;

import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.Parada;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Carga una entrega de ejemplo (en estado EN_TRASLADO) al arrancar el servicio,
 * para poder probar a mano el flujo de "entrega no recibida" sin tener que pasar
 * por toda la planificacion con el proveedor externo de ruteo.
 *
 * Persiste la Ruta con su Parada y la Entrega anidada, más la Entrega en su
 * repositorio independiente, para que la query inversa por entregaId funcione.
 *
 * Es solo para pruebas locales: los repositorios son en memoria, asi que el dato
 * se pierde al reiniciar y se vuelve a crear en el siguiente arranque.
 */
@Component
public class DatosDemoLogistica implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoLogistica.class);

    public static final UUID ENTREGA_DEMO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID RUTA_DEMO_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID CAMION_DEMO_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final EntregaRepositoryInterface entregaRepositorio;
    private final RutaRepositoryInterface rutaRepositorio;

    public DatosDemoLogistica(
            EntregaRepositoryInterface entregaRepositorio,
            RutaRepositoryInterface rutaRepositorio) {
        this.entregaRepositorio = entregaRepositorio;
        this.rutaRepositorio = rutaRepositorio;
    }

    @Override
    public void run(String... args) {
        Camion camion = Camion.builder()
                .id(CAMION_DEMO_ID)
                .patente("AB123CD")
                .build();

        List<Entrega> entregas = new ArrayList<>();

        Parada parada = Parada.builder()
                .id(UUID.fromString("77777777-7777-7777-7777-777777777777"))
                .orden(1)
                .direccion(Direccion.builder()
                        .calle("Av. Medrano")
                        .numero(951)
                        .localidad("CABA")
                        .provincia("Buenos Aires")
                        .codigoPostal("C1179")
                        .build())
                .idEntidadBeneficiaria(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .entregas(entregas)
                .build();

        Entrega entrega = Entrega.builder()
                .id(ENTREGA_DEMO_ID)
                .idDonacion(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .parada(parada)
                .estado(EstadoEntrega.EN_TRASLADO)
                .build();
        entregas.add(entrega);

        Ruta ruta = Ruta.builder()
                .id(RUTA_DEMO_ID)
                .camion(camion)
                .estado(EstadoRuta.INICIADA)
                .paradas(List.of(parada))
                .build();

        entregaRepositorio.guardar(entrega);
        rutaRepositorio.guardar(ruta);

        log.info("[DatosDemoLogistica] Entrega de prueba cargada: {} (estado EN_TRASLADO) en ruta {}",
                ENTREGA_DEMO_ID, RUTA_DEMO_ID);
    }
}
