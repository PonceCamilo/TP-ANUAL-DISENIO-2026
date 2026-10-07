package ar.utn.donatrack.logistica;

import ar.utn.donatrack.logistica.interfaces.repositories.CamionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.EntregaRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.LotePlanificacionRepositoryInterface;
import ar.utn.donatrack.logistica.interfaces.repositories.RutaRepositoryInterface;
import ar.utn.donatrack.logistica.models.comun.Direccion;
import ar.utn.donatrack.logistica.models.entrega.CambioEstadoEntrega;
import ar.utn.donatrack.logistica.models.entrega.Entrega;
import ar.utn.donatrack.logistica.models.entrega.EstadoEntrega;
import ar.utn.donatrack.logistica.models.flota.Camion;
import ar.utn.donatrack.logistica.models.flota.EstadoCamion;
import ar.utn.donatrack.logistica.models.planificacion.DonacionLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoLote;
import ar.utn.donatrack.logistica.models.planificacion.EstadoRuta;
import ar.utn.donatrack.logistica.models.planificacion.LotePlanificacion;
import ar.utn.donatrack.logistica.models.planificacion.Parada;
import ar.utn.donatrack.logistica.models.planificacion.Ruta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
 * Es solo para pruebas locales. Como los datos quedan en la base, cada dato de
 * ejemplo se crea solo si todavía no existe: reiniciar el servicio no pisa los
 * cambios de estado que se hayan hecho sobre ellos.
 */
@Component
public class DatosDemoLogistica implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoLogistica.class);

    public static final UUID ENTREGA_DEMO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID RUTA_DEMO_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID CAMION_DEMO_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final UUID CAMION_DISPONIBLE_1_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    public static final UUID CAMION_DISPONIBLE_2_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    public static final UUID LOTE_DEMO_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntregaRepositoryInterface entregaRepositorio;
    private final RutaRepositoryInterface rutaRepositorio;
    private final CamionRepositoryInterface camionRepositorio;
    private final LotePlanificacionRepositoryInterface loteRepositorio;

    public DatosDemoLogistica(
            EntregaRepositoryInterface entregaRepositorio,
            RutaRepositoryInterface rutaRepositorio,
            CamionRepositoryInterface camionRepositorio,
            LotePlanificacionRepositoryInterface loteRepositorio) {
        this.entregaRepositorio = entregaRepositorio;
        this.rutaRepositorio = rutaRepositorio;
        this.camionRepositorio = camionRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Override
    @Transactional
    public void run(String... args) {
        cargarEntregaEnTraslado();
        cargarCamionesDisponibles();
    }

    private void cargarCamionesDisponibles() {
        if (camionRepositorio.buscarPorId(CAMION_DISPONIBLE_1_ID) != null) {
            log.info("[DatosDemoLogistica] Camiones DISPONIBLE de ejemplo ya existentes, no se recargan");
            return;
        }
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
        if (entregaRepositorio.buscarPorId(ENTREGA_DEMO_ID) != null) {
            log.info("[DatosDemoLogistica] Entrega de prueba {} ya existente, no se recarga", ENTREGA_DEMO_ID);
            return;
        }
        Camion camion = Camion.builder()
                .id(CAMION_DEMO_ID)
                .patente("AB123CD")
                .capacidadVolumenM3(15)
                .alturaM(2.8)
                .capacidadCargaKg(4000)
                .estado(EstadoCamion.EN_RUTA)
                .build();
        camionRepositorio.guardar(camion);

        UUID idDonacion = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID idEntidad = UUID.fromString("33333333-3333-3333-3333-333333333333");

        // Toda ruta pertenece a un lote de planificación (ruta.id_lote es obligatorio).
        LotePlanificacion lote = LotePlanificacion.builder()
                .id(LOTE_DEMO_ID)
                .camiones(List.of(camion))
                .donaciones(List.of(DonacionLote.builder()
                        .idDonacion(idDonacion)
                        .idEntidadBeneficiaria(idEntidad)
                        .direccionEntrega(direccionDemo())
                        .build()))
                .estado(EstadoLote.COMPLETADO)
                .tokenCorrelacion(UUID.randomUUID().toString())
                .fechaEnvio(LocalDateTime.now())
                .fechaRespuesta(LocalDateTime.now())
                .build();
        loteRepositorio.guardar(lote);

        List<Parada> paradas = new ArrayList<>();
        Ruta ruta = Ruta.builder()
                .id(RUTA_DEMO_ID)
                .lote(lote)
                .camion(camion)
                .estado(EstadoRuta.INICIADA)
                .fechaInicio(LocalDateTime.now())
                .paradas(paradas)
                .build();

        List<Entrega> entregas = new ArrayList<>();
        Parada parada = Parada.builder()
                .id(UUID.fromString("77777777-7777-7777-7777-777777777777"))
                .orden(1)
                .direccion(direccionDemo())
                .idEntidadBeneficiaria(idEntidad)
                .entregas(entregas)
                .build();
        paradas.add(parada);

        Entrega entrega = Entrega.builder()
                .id(ENTREGA_DEMO_ID)
                .idDonacion(idDonacion)
                .idEntidadBeneficiaria(idEntidad)
                .parada(parada)
                .ruta(ruta)
                .camion(camion)
                .estado(EstadoEntrega.EN_TRASLADO)
                .build();
        entrega.getHistorial().add(CambioEstadoEntrega.builder()
                .estado(EstadoEntrega.EN_TRASLADO)
                .observacion("Inicio de ruta")
                .build());
        entregas.add(entrega);

        // La ruta primero: guarda en cascada su parada y la entrega.
        rutaRepositorio.guardar(ruta);
        entregaRepositorio.guardar(entrega);

        log.info("[DatosDemoLogistica] Entrega de prueba cargada: {} (estado EN_TRASLADO) en ruta {}",
                ENTREGA_DEMO_ID, RUTA_DEMO_ID);
    }

    // Una instancia nueva por uso: cada parada o donación de lote tiene su fila en direccion.
    private Direccion direccionDemo() {
        return Direccion.builder()
                .calle("Av. Medrano")
                .numero(951)
                .localidad("CABA")
                .provincia("Buenos Aires")
                .codigoPostal("C1179")
                .build();
    }
}
