package ar.utn.donatrack.incentivos.repositories;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class IncentivosRepositorioEnMemoria {

    private final List<Mision> misiones = new ArrayList<>();
    private final Map<UUID, Donante> perfiles = new ConcurrentHashMap<>();

    public void guardarMision(Mision mision) {
        misiones.add(mision);
    }

    public List<Mision> listarMisiones() {
        return new ArrayList<>(misiones);
    }

    public List<Mision> listarMisionesPorCategoria(CategoriaDonante categoria) {
        return misiones.stream()
                .filter(m -> m.getCategoriaRequerida().getClass().equals(categoria.getClass()))
                .toList();
    }

    public Donante obtenerOCrearPerfil(UUID donanteId) {
        return perfiles.computeIfAbsent(donanteId, id -> {
            Donante d = new Donante();
            d.setId(id);
            return d;
        });
    }

    public void guardarPerfil(Donante perfil) {
        perfiles.put(perfil.getId(), perfil);
    }

    public Optional<Donante> buscarPerfil(UUID donanteId) {
        return Optional.ofNullable(perfiles.get(donanteId));
    }

    public List<UUID> listarTodosLosDonanteIds() {
        return new ArrayList<>(perfiles.keySet());
    }

    public List<Donante> listarPerfiles() {
        return new ArrayList<>(perfiles.values());
    }
}
