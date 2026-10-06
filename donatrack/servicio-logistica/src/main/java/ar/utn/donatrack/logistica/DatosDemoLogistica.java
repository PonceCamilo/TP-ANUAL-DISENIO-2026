package ar.utn.donatrack.logistica;

import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
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
 * Carga datos de ejemplo al arrancar el servicio:
 *
 *  - Una entrega en estado EN_TRASLADO, para poder probar a mano el flujo de
 *    "entrega no recibida" sin pasar por toda la planificación. Su camión
 *    (AB123CD) queda EN_RUTA, porque su ruta ya está iniciada.
 *  - Dos camiones DISPONIBLE, para que la planificación (y el broker de
 *    logística, que planifica sin indicar camiones) funcione desde el arranque.
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
    public static final UUID CAMION_DISPONIBLE_1_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    public static final UUID CAMION_DISPONIBLE_2_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private final EntregaRepositoryInterface entregaRepositorio;
    private final RutaRepositoryInterface rutaRepositorio;
    private final CamionRepositoryInterface camionRepositorio;

    public DatosDemoLogistica(
            EntregaRepositoryInterface entregaRepositorio,
            RutaRepositoryInterface rutaRepositorio,
            CamionRepositoryInterface camionRepositorio) {
        this.entregaRepositorio = entregaRepositorio;
        this.rutaRepositorio = rutaRepositorio;
        this.camionRepositorio = camionRepositorio;
    }

    @Override
    public void run(String... args) {
        cargarEntregaEnTraslado();
        cargarCamionesDisponibles();
    }

    private void cargarCamionesDisponibles() {
        camionRepositorio.guardar(Camion.builder()
                .id(CAMION_DISPONIBLE_1_ID)
                .patente("AC456EF")
                .capacidadVolumenM3(20)
                .alturaM(3)
                .capacidadCargaKg(5000)
                .build());
        camionRepositorio.guardar(Camion.builder()
                .id(CAMION_DISPONIBLE_2_ID)
                .patente("AD789GH")
                .capacidadVolumenM3(12)
                .alturaM(2.5)
                .capacidadCargaKg(3000)
                .build());

        log.info("[DatosDemoLogistica] Camiones DISPONIBLE cargados: {} (AC456EF), {} (AD789GH)",
                CAMION_DISPONIBLE_1_ID, CAMION_DISPONIBLE_2_ID);
    }

    private void cargarEntregaEnTraslado() {
        Camion camion = Camion.builder()
                .id(CAMION_DEMO_ID)
                .patente("AB123CD")
                .capacidadVolumenM3(15)
                .alturaM(2.8)
                .capacidadCargaKg(4000)
                .estado(EstadoCamion.EN_RUTA)
                .build();
        camionRepositorio.guardar(camion);

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
