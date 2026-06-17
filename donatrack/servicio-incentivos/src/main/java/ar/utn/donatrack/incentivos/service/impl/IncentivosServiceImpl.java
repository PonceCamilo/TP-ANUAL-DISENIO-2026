package ar.utn.donatrack.incentivos.service.impl;

import ar.utn.donatrack.incentivos.client.NotificacionClient;
import ar.utn.donatrack.incentivos.model.*;
import ar.utn.donatrack.incentivos.repository.impl.IncentivosRepositorioEnMemoria;
import ar.utn.donatrack.incentivos.service.IncentivosService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IncentivosServiceImpl implements IncentivosService {

    private static final Logger log = LoggerFactory.getLogger(IncentivosServiceImpl.class);

    private final IncentivosRepositorioEnMemoria repositorio;
    private final NotificacionClient notificacionClient;

    public IncentivosServiceImpl(IncentivosRepositorioEnMemoria repositorio,
                                 NotificacionClient notificacionClient) {
        this.repositorio        = repositorio;
        this.notificacionClient = notificacionClient;
    }

    // ── Carga misiones base al arrancar la aplicación ─────────────────────────
    @PostConstruct
    public void inicializarMisiones() {
        List<Mision> misionesBase = List.of(
            new Mision("Primera donación", "Realizá tu primera donación", TipoMision.DONACIONES_EXITOSAS, CategoriaDonante.COLABORADOR, 1, 1),
            new Mision("Tres donaciones", "Realizá 3 donaciones exitosas", TipoMision.DONACIONES_EXITOSAS, CategoriaDonante.COLABORADOR, 3, 2),
            new Mision("Variedad solidaria", "Doná de 3 categorías distintas", TipoMision.COMPLETITUD, CategoriaDonante.COLABORADOR, 3, 3),
            new Mision("Donación generosa", "Realizá una donación con más de 20 bienes", TipoMision.HABIL_DONADOR, CategoriaDonante.SOSTENEDOR, 20, 1),
            new Mision("Racha mensual", "Doná durante 3 meses consecutivos", TipoMision.RACHA, CategoriaDonante.SOSTENEDOR, 3, 2),
            new Mision("Gran impacto", "Logra 10 donaciones exitosas", TipoMision.DONACIONES_EXITOSAS, CategoriaDonante.TRANSFORMADOR, 10, 1)
        );
        misionesBase.forEach(repositorio::guardarMision);
        log.info("Misiones base cargadas: {}", misionesBase.size());
    }

    // ── Consultas (endpoints GET) ─────────────────────────────────────────────

    @Override
    public PerfilDonante obtenerMetricas(UUID donanteId) {
        return repositorio.obtenerOCrearPerfil(donanteId);
    }

    @Override
    public List<ProgresoMision> obtenerMisiones(UUID donanteId) {
        PerfilDonante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        asegurarProgresosMisionesInicializados(donanteId, perfil.getCategoria());
        return repositorio.listarProgresosDonante(donanteId);
    }

    @Override
    public List<Insignia> obtenerInsignias(UUID donanteId) {
        return repositorio.listarInsigniasDonante(donanteId);
    }

    // ── Procesamiento de eventos de donación ─────────────────────────────────

    /**
     * Llamado desde servicio-donaciones cuando el donante registra una donación.
     * Actualiza métricas y verifica si se completan misiones.
     */
    @Override
    public void procesarDonacion(UUID donanteId, String destinatario, String medio,
                                 int cantidadBienes, List<String> categoriasDonadas) {

        PerfilDonante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        perfil.registrarDonacion();
        repositorio.guardarPerfil(perfil);

        asegurarProgresosMisionesInicializados(donanteId, perfil.getCategoria());

        // ── Verificar misiones de tipo HABIL_DONADOR ──────────────────────────
        if (cantidadBienes > 0) {
            verificarMisionesDeTipo(donanteId, destinatario, medio, TipoMision.HABIL_DONADOR, cantidadBienes, perfil);
        }

        // ── Verificar misiones de tipo COMPLETITUD ────────────────────────────
        // (cantidad de categorías distintas en esta donación)
        verificarMisionesDeTipo(donanteId, destinatario, medio, TipoMision.COMPLETITUD,
                categoriasDonadas.size(), perfil);
    }

    /**
     * Llamado desde servicio-donaciones cuando una donación fue entregada exitosamente.
     */
    @Override
    public void procesarDonacionExitosa(UUID donanteId, String destinatario, String medio) {
        PerfilDonante perfil = repositorio.obtenerOCrearPerfil(donanteId);
        perfil.registrarOrganizacionAyudada();
        repositorio.guardarPerfil(perfil);

        asegurarProgresosMisionesInicializados(donanteId, perfil.getCategoria());

        // ── Verificar misiones de tipo DONACIONES_EXITOSAS ────────────────────
        verificarMisionesDeTipo(donanteId, destinatario, medio, TipoMision.DONACIONES_EXITOSAS, 1, perfil);
    }

    // ── Lógica interna ────────────────────────────────────────────────────────

    /**
     * Avanza el progreso en todas las misiones del tipo dado y verifica completado.
     * Si se completa una misión: otorga insignia + notifica + verifica cambio de categoría.
     */
    private void verificarMisionesDeTipo(UUID donanteId, String destinatario, String medio,
                                          TipoMision tipo, int incremento, PerfilDonante perfil) {

        repositorio.listarProgresosDonante(donanteId).stream()
                .filter(p -> !p.isCompletada())
                .filter(p -> p.getMision().getTipo() == tipo)
                .filter(p -> p.getMision().getCategoriaRequerida() == perfil.getCategoria())
                .forEach(progreso -> {
                    boolean completada = progreso.incrementarProgreso(incremento);
                    repositorio.guardarProgreso(progreso);

                    if (completada) {
                        // ── MISIÓN COMPLETADA ─────────────────────────────────────────────
                        log.info("Donante {} completó la misión: {}", donanteId, progreso.getMision().getNombre());

                        // 1. Otorgar insignia
                        Insignia insignia = new Insignia(donanteId, progreso.getMision());
                        repositorio.guardarInsignia(insignia);

                        // ══════════════════════════════════════════════════════════════════
                        // DISPARO DE NOTIFICACIÓN: donante cumplió una misión
                        // (intervención requerida por la consigna)
                        // ══════════════════════════════════════════════════════════════════
                        notificacionClient.enviarNotificacion(
                                destinatario,
                                "¡Felicitaciones! Completaste la misión '"
                                        + progreso.getMision().getNombre()
                                        + "' y obtuviste una nueva insignia.",
                                medio
                        );

                        // 2. Verificar si subió de categoría (todas las misiones de su categoría completadas)
                        verificarCambioDeCategoria(donanteId, destinatario, medio, perfil);
                    }
                });
    }

    /**
     * Si el donante completó todas las misiones de su categoría actual, sube de categoría.
     */
    private void verificarCambioDeCategoria(UUID donanteId, String destinatario,
                                             String medio, PerfilDonante perfil) {

        List<ProgresoMision> progresosCategoriaActual = repositorio
                .listarProgresosDonante(donanteId).stream()
                .filter(p -> p.getMision().getCategoriaRequerida() == perfil.getCategoria())
                .toList();

        boolean todasCompletadas = !progresosCategoriaActual.isEmpty()
                && progresosCategoriaActual.stream().allMatch(ProgresoMision::isCompletada);

        if (todasCompletadas) {
            boolean subio = perfil.subirCategoria();
            if (subio) {
                repositorio.guardarPerfil(perfil);
                log.info("Donante {} subió a la categoría: {}", donanteId, perfil.getCategoria());

                // ══════════════════════════════════════════════════════════════════
                // DISPARO DE NOTIFICACIÓN: donante cambió de categoría
                // (intervención requerida por la consigna)
                // ══════════════════════════════════════════════════════════════════
                notificacionClient.enviarNotificacion(
                        destinatario,
                        "¡Subiste de categoría! Ahora sos " + perfil.getCategoria()
                                + ". ¡Seguí donando para seguir creciendo!",
                        medio
                );

                // Inicializar progresos para la nueva categoría
                asegurarProgresosMisionesInicializados(donanteId, perfil.getCategoria());
            }
        }
    }

    /**
     * Crea los ProgresoMision para el donante si todavía no existen para su categoría.
     */
    private void asegurarProgresosMisionesInicializados(UUID donanteId, CategoriaDonante categoria) {
        List<Mision> misionesCat = repositorio.listarMisionesPorCategoria(categoria);
        List<ProgresoMision> existentes = repositorio.listarProgresosDonante(donanteId);
        Set<UUID> misionesConProgreso = new HashSet<>();
        existentes.forEach(p -> misionesConProgreso.add(p.getMision().getId()));

        misionesCat.stream()
                .filter(m -> !misionesConProgreso.contains(m.getId()))
                .forEach(m -> repositorio.guardarProgreso(new ProgresoMision(donanteId, m)));
    }
}
