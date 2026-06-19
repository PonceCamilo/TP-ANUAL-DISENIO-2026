package ar.utn.donatrack.incentivos.services.impl;

import ar.utn.donatrack.incentivos.client.N8nWebhookClient;
import ar.utn.donatrack.incentivos.client.NotificacionClient;
import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Colaborador;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Sostenedor;
import ar.utn.donatrack.incentivos.models.categoriasdonante.Transformador;
import ar.utn.donatrack.incentivos.models.insignias.Insignia;
import ar.utn.donatrack.incentivos.models.insignias.InsigniaObtenida;
import ar.utn.donatrack.incentivos.models.misiones.*;
import ar.utn.donatrack.incentivos.repositories.impl.IncentivosRepositorioEnMemoria;
import ar.utn.donatrack.incentivos.services.IncentivosService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IncentivosServiceImpl implements IncentivosService {

    private static final Logger log = LoggerFactory.getLogger(IncentivosServiceImpl.class);
    private final IncentivosRepositorioEnMemoria repositorio;
    private final NotificacionClient notificacionClient;
    private final N8nWebhookClient n8nWebhookClient;

    public IncentivosServiceImpl(IncentivosRepositorioEnMemoria repositorio, NotificacionClient notificacionClient,
                                 N8nWebhookClient n8nWebhookClient) {
        this.repositorio = repositorio;
        this.notificacionClient = notificacionClient;
        this.n8nWebhookClient = n8nWebhookClient;
    }

    @PostConstruct
    public void inicializarMisiones() {
        // Misión 1 - Para Colaborador
        Mision m1 = new DonacionesExitosas();
        m1.setNombre("Primera donación");
        m1.setCategoriaRequerida(new Colaborador());
        m1.setObjetivo(1);
        Insignia ins1 = new Insignia();
        ins1.setId(UUID.randomUUID());
        ins1.setNombre("Semilla de Solidaridad");
        ins1.setImagen("semilla.png");
        m1.setInsignia(ins1);
        repositorio.guardarMision(m1);

        // Misión 2 - Para Sostenedor
        Mision m2 = new HabilDonador();
        m2.setNombre("Gran Corazón");
        m2.setCategoriaRequerida(new Sostenedor());
        m2.setObjetivo(10); // Requiere donar 10 bienes de una vez
        Insignia ins2 = new Insignia();
        ins2.setId(UUID.randomUUID());
        ins2.setNombre("Corazón de Plata");
        ins2.setImagen("plata.png");
        m2.setInsignia(ins2);
        repositorio.guardarMision(m2);

        // Misión 3 - Para Transformador
        Mision m3 = new Completitud();
        m3.setNombre("Ayuda Integral");
        m3.setCategoriaRequerida(new Transformador());
        m3.setObjetivo(3); // Requiere donar de 3 categorías distintas
        Insignia ins3 = new Insignia();
        ins3.setId(UUID.randomUUID());
        ins3.setNombre("Estrella Dorada");
        ins3.setImagen("oro.png");
        m3.setInsignia(ins3);
        repositorio.guardarMision(m3);

        log.info("Misiones base del sistema cargadas correctamente.");
    }

    private Donante obtenerOCrearPerfilConInit(UUID donanteId) {
        Donante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        if (perfil.getCategoria() == null) {
            perfil.setCategoria(new Colaborador());
            List<Mision> misionesColab = repositorio.listarMisionesPorCategoria(perfil.getCategoria());
            if (!misionesColab.isEmpty()) {
                perfil.setMisionActual(misionesColab.get(0));
            }
        }
        return perfil;
    }

    @Override
    public Donante obtenerMetricas(UUID donanteId) {
        return obtenerOCrearPerfilConInit(donanteId);
    }

    @Override
    public List<Mision> obtenerMisiones(UUID donanteId) {
        Donante perfil = obtenerOCrearPerfilConInit(donanteId);
        return repositorio.listarMisionesPorCategoria(perfil.getCategoria());
    }

    @Override
    public List<InsigniaObtenida> obtenerInsignias(UUID donanteId) {
        return obtenerOCrearPerfilConInit(donanteId).getInsigniasObtenidas();
    }

    @Override
    public void procesarDonacion(UUID donanteId, String destinatario, String medio, int cantidadBienes, List<String> categoriasDonadas) {
        Donante perfil = obtenerOCrearPerfilConInit(donanteId);
        perfil.registrarDonacion(cantidadBienes, categoriasDonadas);
        repositorio.guardarPerfil(perfil);
        verificarMisionActiva(perfil, destinatario, medio);
    }

    @Override
    public void procesarDonacionExitosa(UUID donanteId, String destinatario, String medio) {
        Donante perfil = obtenerOCrearPerfilConInit(donanteId);
        perfil.registrarOrganizacionAyudada();
        repositorio.guardarPerfil(perfil);
        verificarMisionActiva(perfil, destinatario, medio);
    }

    // CORRECCIÓN: Lógica para avanzar la misión correctamente
    private void verificarMisionActiva(Donante perfil, String destinatario, String medio) {
        Mision activa = perfil.getMisionActual();

        // Si no hay misión o no la completó, no hacemos nada
        if (activa == null || !activa.estaCompletada(perfil)) {
            return;
        }

        // 1. Damos la insignia una sola vez
        InsigniaObtenida nueva = new InsigniaObtenida(activa.getInsignia(), true);
        perfil.addInsignia(nueva);
        notificacionClient.enviarNotificacion(destinatario, "¡Misión cumplida!: " + activa.getNombre(), medio, "MISION_CUMPLIDA");
        n8nWebhookClient.notificarInsigniaObtenida(perfil.getId(), nueva, destinatario);

        // 2. Calculamos cómo avanzar
        List<Mision> misionesDeSuCategoria = repositorio.listarMisionesPorCategoria(perfil.getCategoria());
        int index = misionesDeSuCategoria.indexOf(activa);

        if (index != -1 && index < misionesDeSuCategoria.size() - 1) {
            // Le pasamos la SIGUIENTE misión de su misma categoría
            perfil.setMisionActual(misionesDeSuCategoria.get(index + 1));
        } else {
            // No le quedan misiones -> Sube de categoría
            if (perfil.subirCategoria()) {
                notificacionClient.enviarNotificacion(destinatario, "¡Subiste a " + perfil.getCategoria().getClass().getSimpleName() + "!", medio, "CAMBIO_CATEGORIA");

                // Le damos la primera misión de su nueva categoría
                List<Mision> misionesNuevaCat = repositorio.listarMisionesPorCategoria(perfil.getCategoria());
                perfil.setMisionActual(misionesNuevaCat.isEmpty() ? null : misionesNuevaCat.get(0));
            } else {
                // Llegó a Transformador y terminó el juego
                perfil.setMisionActual(null);
            }
        }

        repositorio.guardarPerfil(perfil);
    }
}