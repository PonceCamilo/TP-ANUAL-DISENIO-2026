package ar.utn.donatrack.incentivos.repository.impl;

import ar.utn.donatrack.incentivos.model.*;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria para Entrega 2.
 * En Entrega 4 se reemplaza por JPA sin cambiar el servicio.
 */
@Repository
public class IncentivosRepositorioEnMemoria {

    // ── Misiones (se cargan al arrancar) ─────────────────────────────────────
    private final Map<UUID, Mision> misiones = new ConcurrentHashMap<>();

    // ── Progreso por donante ──────────────────────────────────────────────────
    // Clave: donanteId → (misionId → ProgresoMision)
    private final Map<UUID, Map<UUID, ProgresoMision>> progresos = new ConcurrentHashMap<>();

    // ── Insignias por donante ─────────────────────────────────────────────────
    private final Map<UUID, List<Insignia>> insignias = new ConcurrentHashMap<>();

    // ── Perfil por donante ────────────────────────────────────────────────────
    private final Map<UUID, PerfilDonante> perfiles = new ConcurrentHashMap<>();

    // ── Misiones ──────────────────────────────────────────────────────────────

    public void guardarMision(Mision mision) {
        misiones.put(mision.getId(), mision);
    }

    public List<Mision> listarMisiones() {
        return new ArrayList<>(misiones.values());
    }

    public List<Mision> listarMisionesPorCategoria(CategoriaDonante categoria) {
        return misiones.values().stream()
                .filter(m -> m.getCategoriaRequerida() == categoria)
                .sorted(Comparator.comparingInt(Mision::getOrden))
                .toList();
    }

    // ── Progreso ──────────────────────────────────────────────────────────────

    public void guardarProgreso(ProgresoMision progreso) {
        progresos.computeIfAbsent(progreso.getDonanteId(), k -> new ConcurrentHashMap<>())
                .put(progreso.getMision().getId(), progreso);
    }

    public List<ProgresoMision> listarProgresosDonante(UUID donanteId) {
        return new ArrayList<>(
                progresos.getOrDefault(donanteId, Map.of()).values()
        );
    }

    public Optional<ProgresoMision> buscarProgreso(UUID donanteId, UUID misionId) {
        return Optional.ofNullable(
                progresos.getOrDefault(donanteId, Map.of()).get(misionId)
        );
    }

    // ── Insignias ─────────────────────────────────────────────────────────────

    public void guardarInsignia(Insignia insignia) {
        insignias.computeIfAbsent(insignia.getDonanteId(), k -> new ArrayList<>())
                .add(insignia);
    }

    public List<Insignia> listarInsigniasDonante(UUID donanteId) {
        return Collections.unmodifiableList(
                insignias.getOrDefault(donanteId, List.of())
        );
    }

    // ── Perfil ────────────────────────────────────────────────────────────────

    public PerfilDonante obtenerOCrearPerfil(UUID donanteId) {
        return perfiles.computeIfAbsent(donanteId, PerfilDonante::new);
    }

    public void guardarPerfil(PerfilDonante perfil) {
        perfiles.put(perfil.getDonanteId(), perfil);
    }

    public Optional<PerfilDonante> buscarPerfil(UUID donanteId) {
        return Optional.ofNullable(perfiles.get(donanteId));
    }
}
