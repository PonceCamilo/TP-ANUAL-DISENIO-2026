package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.client.N8nWebhookClient;
import ar.utn.donatrack.incentivos.client.NotificacionClient;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.PosicionRanking;
import ar.utn.donatrack.incentivos.models.RankingMensual;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Sostenedor;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Transformador;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.Completitud;
import ar.utn.donatrack.incentivos.models.misiones.DonacionesExitosas;
import ar.utn.donatrack.incentivos.models.misiones.HabilDonador;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.models.misiones.Racha;
import ar.utn.donatrack.incentivos.interfaces.services.IncentivosServiceInterface;
import ar.utn.donatrack.incentivos.repositories.IncentivosRepositorioEnMemoria;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class IncentivosService implements IncentivosServiceInterface {

    private static final Logger log = LoggerFactory.getLogger(IncentivosService.class);
    private final IncentivosRepositorioEnMemoria repositorio;
    private final NotificacionClient notificacionClient;
    private final N8nWebhookClient n8nWebhookClient;

    public IncentivosService(IncentivosRepositorioEnMemoria repositorio,
                             NotificacionClient notificacionClient,
                             N8nWebhookClient n8nWebhookClient) {
        this.repositorio = repositorio;
        this.notificacionClient = notificacionClient;
        this.n8nWebhookClient = n8nWebhookClient;
    }

    @PostConstruct
    public void inicializarMisiones() {
        repositorio.guardarMision(new DonacionesExitosas(
                "Primera donacion exitosa",
                "Completar una donacion entregada a destino.",
                new Colaborador(),
                1,
                Insignia.builder()
                        .id(UUID.randomUUID())
                        .nombre("Semilla de Solidaridad")
                        .imagen("semilla.png")
                        .build()
        ));
        repositorio.guardarMision(new Racha(
                "Racha solidaria",
                "Donar al menos una vez por mes durante tres meses seguidos.",
                new Colaborador(),
                3,
                Insignia.builder()
                        .id(UUID.randomUUID())
                        .nombre("Constancia Solidaria")
                        .imagen("racha.png")
                        .build()
        ));
        repositorio.guardarMision(new HabilDonador(
                "Gran Corazon",
                "Donar diez bienes en una unica donacion.",
                new Sostenedor(),
                10,
                Insignia.builder()
                        .id(UUID.randomUUID())
                        .nombre("Corazon de Plata")
                        .imagen("plata.png")
                        .build()
        ));
        repositorio.guardarMision(new Completitud(
                "Ayuda Integral",
                "Donar bienes de tres categorias distintas.",
                new Transformador(),
                3,
                Insignia.builder()
                        .id(UUID.randomUUID())
                        .nombre("Estrella Dorada")
                        .imagen("oro.png")
                        .build()
        ));

        log.info("Misiones base del sistema cargadas correctamente.");
    }

    private Donante obtenerOCrearPerfilConInit(UUID donanteId) {
        Donante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        if (perfil.getCategoria() == null) {
            perfil.setCategoria(new Colaborador());
        }
        if (perfil.getProgresoMision().getMisionActual() == null) {
            asignarPrimeraMisionDeCategoria(perfil);
        }
        revisarPerdidaMision(perfil);
        repositorio.guardarPerfil(perfil);
        return perfil;
    }

    public Donante obtenerMetricas(UUID donanteId) {
        return obtenerOCrearPerfilConInit(donanteId);
    }

    public List<Mision> obtenerMisiones(UUID donanteId) {
        Donante perfil = obtenerOCrearPerfilConInit(donanteId);
        return repositorio.listarMisionesPorCategoria(perfil.getCategoria());
    }

    public List<InsigniaObtenida> obtenerInsignias(UUID donanteId) {
        return obtenerOCrearPerfilConInit(donanteId).getInsigniasObtenidas();
    }

    public RankingMensual obtenerRankingMensualActual() {
        LocalDate hoy = LocalDate.now();
        int mes = hoy.getMonthValue();
        int anio = hoy.getYear();
        LocalDateTime primerDiaDelMes = hoy.withDayOfMonth(1).atStartOfDay();

        List<Donante> donantes = repositorio.listarPerfiles();
        List<PosicionRanking> posiciones = donantes.stream()
                .map(donante -> PosicionRanking.builder()
                        .donanteId(donante.getId())
                        .misionesCompletadas(misionesCompletadasEnPeriodo(donante, mes, anio))
                        .donacionesMesActual(donante.getMetricas().donacionesMesActual())
                        .totalDonacionesHistoricas(donante.getMetricas().totalDonacionesHistoricas())
                        .primerDiaDelMes(primerDiaDelMes)
                        .build())
                .sorted(Comparator.comparingInt(PosicionRanking::getMisionesCompletadas).reversed()
                        .thenComparing(Comparator.comparingInt(PosicionRanking::getDonacionesMesActual).reversed())
                        .thenComparing(Comparator.comparingInt(PosicionRanking::getTotalDonacionesHistoricas).reversed()))
                .toList();

        for (int i = 0; i < posiciones.size(); i++) {
            posiciones.get(i).setPuesto(i + 1);
        }

        return RankingMensual.builder()
                .mes(mes)
                .anio(anio)
                .fechaCalculo(hoy)
                .posiciones(posiciones)
                .build();
    }

    public int obtenerPosicionRankingActual(UUID donanteId) {
        obtenerOCrearPerfilConInit(donanteId);
        return obtenerRankingMensualActual().posicionDe(donanteId);
    }

    public void procesarDonacion(UUID donanteId, String destinatario, String medio, int cantidadBienes, List<String> categoriasDonadas) {
        Donante perfil = obtenerOCrearPerfilConInit(donanteId);
        perfil.registrarDonacion(cantidadBienes, categoriasDonadas);
        repositorio.guardarPerfil(perfil);
        verificarMisionActiva(perfil, destinatario, medio);
    }

    public void procesarDonacionExitosa(UUID donanteId, String destinatario, String medio) {
        Donante perfil = obtenerOCrearPerfilConInit(donanteId);
        perfil.registrarOrganizacionAyudada();
        repositorio.guardarPerfil(perfil);
        verificarMisionActiva(perfil, destinatario, medio);
    }

    private void verificarMisionActiva(Donante perfil, String destinatario, String medio) {
        Mision activa = perfil.getProgresoMision().getMisionActual();
        if (activa == null || !activa.estaCompletada(perfil)) {
            return;
        }

        InsigniaObtenida nueva = activa.otorgarInsignia();
        perfil.addInsignia(nueva);
        notificacionClient.enviarNotificacion(destinatario, "Mision cumplida: " + activa.getNombre(), medio, "MISION_CUMPLIDA");
        n8nWebhookClient.notificarInsigniaObtenida(perfil.getId(), nueva, destinatario);

        avanzarMision(perfil, activa, destinatario, medio);
        repositorio.guardarPerfil(perfil);
    }

    private void avanzarMision(Donante perfil, Mision activa, String destinatario, String medio) {
        List<Mision> misionesDeSuCategoria = repositorio.listarMisionesPorCategoria(perfil.getCategoria());
        int index = misionesDeSuCategoria.indexOf(activa);

        if (index != -1 && index < misionesDeSuCategoria.size() - 1) {
            perfil.getProgresoMision().setMisionActual(misionesDeSuCategoria.get(index + 1));
            return;
        }

        if (perfil.subirCategoria()) {
            notificacionClient.enviarNotificacion(destinatario, "Subiste a " + perfil.getCategoria().getClass().getSimpleName() + ".", medio, "CAMBIO_CATEGORIA");
            asignarPrimeraMisionDeCategoria(perfil);
            return;
        }

        perfil.getProgresoMision().setMisionActual(null);
    }

    private void asignarPrimeraMisionDeCategoria(Donante perfil) {
        List<Mision> misiones = repositorio.listarMisionesPorCategoria(perfil.getCategoria());
        perfil.getProgresoMision().setMisionActual(misiones.isEmpty() ? null : misiones.get(0));
    }

    private void revisarPerdidaMision(Donante perfil) {
        if (perfil.getProgresoMision().getMisionActual() instanceof Racha
                && perfil.getMetricas().pasoUnMesCompletoSinDonaciones(LocalDate.now())) {
            asignarPrimeraMisionDeCategoria(perfil);
        }
    }

    private int misionesCompletadasEnPeriodo(Donante donante, int mes, int anio) {
        return (int) donante.getInsigniasObtenidas().stream()
                .filter(insignia -> insignia.getFechaObtencion() != null)
                .filter(insignia -> insignia.getFechaObtencion().getMonthValue() == mes)
                .filter(insignia -> insignia.getFechaObtencion().getYear() == anio)
                .count();
    }
}
