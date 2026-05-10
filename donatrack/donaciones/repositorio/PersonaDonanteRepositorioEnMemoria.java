package com.donatrack.donaciones.repositorio;

import com.donatrack.donaciones.dominio.PersonaDonante;
import com.donatrack.donaciones.dominio.contacto.TipoMedioContacto;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del repositorio de personas donantes.
 * Utilizada en Entrega 1 (sin persistencia formal).
 *
 * Usa ConcurrentHashMap para ser thread-safe en un entorno web con múltiples requests.
 * En Entrega 4 esta clase será reemplazada por PersonaDonanteRepositorioJPA.
 */
@Repository
public class PersonaDonanteRepositorioEnMemoria implements PersonaDonanteRepositorio {

    private final Map<UUID, PersonaDonante> almacenamiento = new ConcurrentHashMap<>();

    @Override
    public PersonaDonante guardar(PersonaDonante personaDonante) {
        almacenamiento.put(personaDonante.getId(), personaDonante);
        return personaDonante;
    }

    @Override
    public Optional<PersonaDonante> buscarPorId(UUID id) {
        return Optional.ofNullable(almacenamiento.get(id));
    }

    @Override
    public Optional<PersonaDonante> buscarPorEmail(String email) {
        return almacenamiento.values().stream()
                .filter(p -> p.getMediosDeContacto().stream()
                        .anyMatch(m -> m.getTipo() == TipoMedioContacto.EMAIL
                                && m.getValor().equalsIgnoreCase(email)))
                .findFirst();
    }

    @Override
    public List<PersonaDonante> buscarTodos() {
        return new ArrayList<>(almacenamiento.values());
    }

    @Override
    public List<PersonaDonante> buscarActivos() {
        return almacenamiento.values().stream()
                .filter(PersonaDonante::isActivo)
                .toList();
    }

    @Override
    public void eliminar(UUID id) {
        almacenamiento.remove(id);
    }

    @Override
    public boolean existePorEmail(String email) {
        return buscarPorEmail(email).isPresent();
    }
}
