package ar.utn.donatrack.incentivos.repositories;

import ar.utn.donatrack.incentivos.models.Donante;
import ar.utn.donatrack.incentivos.models.categoriasdonante.CategoriaDonante;
import ar.utn.donatrack.incentivos.models.misiones.Mision;
import ar.utn.donatrack.incentivos.repositories.jpa.CategoriaDonanteJpaRepository;
import ar.utn.donatrack.incentivos.repositories.jpa.DonanteJpaRepository;
import ar.utn.donatrack.incentivos.repositories.jpa.MisionJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class IncentivosRepository {

    private MisionJpaRepository misionRepository;
    private DonanteJpaRepository donanteRepository;
    private CategoriaDonanteJpaRepository categoriaRepository;
    private final List<Mision> misionesFallback = new ArrayList<>();
    private final Map<UUID, Donante> perfilesFallback = new ConcurrentHashMap<>();

    /**
     * Constructor sin repositorios: activa el modo en memoria.
     *
     * Existe únicamente para IncentivosServiceTest, que prueba las reglas de
     * negocio sin levantar una base. NO lo usa la aplicación.
     */
    public IncentivosRepository() {
    }

    /**
     * Constructor real, el que usa la aplicación.
     *
     * EL @Autowired NO ES OPCIONAL ACÁ. Cuando una clase tiene varios
     * constructores y ninguno está anotado, Spring elige el que no recibe
     * argumentos. Sin esta anotación, la aplicación arrancaba con los tres
     * repositorios en null, caía en el modo en memoria y NO PERSISTÍA NADA:
     * los endpoints devolvían 201, las tablas existían vacías y nada fallaba
     * a la vista. Lo detectó MapeoIncentivosTest.
     */
    @Autowired
    public IncentivosRepository(MisionJpaRepository misionRepository, DonanteJpaRepository donanteRepository, CategoriaDonanteJpaRepository categoriaRepository) {
        this.misionRepository = misionRepository;
        this.donanteRepository = donanteRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public void guardarMision(Mision mision) {
        if (usaFallbackEnMemoria()) {
            if (mision.getOrden() == 0) {
                mision.setOrden((int) misionesFallback.stream()
                        .filter(m -> mismaCategoria(m.getCategoriaRequerida(), mision.getCategoriaRequerida()))
                        .count() + 1);
            }
            misionesFallback.add(mision);
            return;
        }

        if (misionRepository.existsByNombre(mision.getNombre())) {
            return;
        }

        CategoriaDonante categoria = guardarORecuperarCategoria(mision.getCategoriaRequerida());
        mision.setCategoriaRequerida(categoria);
        mision.setOrden((int) misionRepository.countByCategoriaRequeridaOrden(categoria.getOrden()) + 1);
        misionRepository.save(mision);
    }

    public List<Mision> listarMisiones() {
        if (usaFallbackEnMemoria()) {
            return new ArrayList<>(misionesFallback);
        }
        return misionRepository.findAll();
    }

    public List<Mision> listarMisionesPorCategoria(CategoriaDonante categoria) {
        if (categoria == null) {
            return List.of();
        }
        if (usaFallbackEnMemoria()) {
            return misionesFallback.stream()
                    .filter(m -> mismaCategoria(m.getCategoriaRequerida(), categoria))
                    .sorted(Comparator.comparingInt(Mision::getOrden))
                    .toList();
        }
        return misionRepository.findByCategoriaRequeridaOrdenOrderByOrdenAsc(categoria.getOrden());
    }

    public Donante obtenerOCrearPerfil(UUID donanteId) {
        if (usaFallbackEnMemoria()) {
            return perfilesFallback.computeIfAbsent(donanteId, id -> {
                Donante d = new Donante();
                d.setId(id);
                return d;
            });
        }
        return donanteRepository.findById(donanteId).orElseGet(() -> {
            Donante d = new Donante();
            d.setId(donanteId);
            return d;
        });
    }

    @Transactional
    public void guardarPerfil(Donante perfil) {
        if (usaFallbackEnMemoria()) {
            perfilesFallback.put(perfil.getId(), perfil);
            return;
        }
        if (perfil.getCategoria() != null) {
            perfil.setCategoria(guardarORecuperarCategoria(perfil.getCategoria()));
        }
        donanteRepository.save(perfil);
    }

    public Optional<Donante> buscarPerfil(UUID donanteId) {
        if (usaFallbackEnMemoria()) {
            return Optional.ofNullable(perfilesFallback.get(donanteId));
        }
        return donanteRepository.findById(donanteId);
    }

    public List<UUID> listarTodosLosDonanteIds() {
        if (usaFallbackEnMemoria()) {
            return new ArrayList<>(perfilesFallback.keySet());
        }
        return donanteRepository.findAll().stream()
                .map(Donante::getId)
                .toList();
    }

    public List<Donante> listarPerfiles() {
        if (usaFallbackEnMemoria()) {
            return new ArrayList<>(perfilesFallback.values());
        }
        return donanteRepository.findAll();
    }

    private CategoriaDonante guardarORecuperarCategoria(CategoriaDonante categoria) {
        return categoriaRepository.findById(categoria.getOrden())
                .orElseGet(() -> categoriaRepository.save(categoria));
    }

    private boolean usaFallbackEnMemoria() {
        return misionRepository == null || donanteRepository == null || categoriaRepository == null;
    }

    private boolean mismaCategoria(CategoriaDonante unaCategoria, CategoriaDonante otraCategoria) {
        return unaCategoria != null
                && otraCategoria != null
                && unaCategoria.getClass().equals(otraCategoria.getClass());
    }
}
