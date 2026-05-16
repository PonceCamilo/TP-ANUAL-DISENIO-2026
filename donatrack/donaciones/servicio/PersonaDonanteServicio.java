package com.donatrack.donaciones.repositorio;

import com.donatrack.donaciones.dominio.PersonaDonante;
import com.donatrack.donaciones.dominio.contacto.TipoMedioContacto;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ═══════════════════════════════════════════════════════════════════════════════
// PATRÓN: SINGLETON
// ───────────────────────────────────────────────────────────────────────────────
// Por qué: el almacenamiento en memoria ES el estado de la aplicación mientras
// no hay base de datos real. Si se crearan dos instancias de este repositorio,
// cada una tendría su propio Map y los datos quedarían partidos: un request
// guardaría en una instancia y otro no los encontraría.
//
// Spring ya garantiza scope singleton con @Repository, pero lo hacemos explícito
// documentando el patrón para que quede claro en el diagrama de clases y en la
// justificación de diseño de la entrega.
//
// Beneficio: una única fuente de verdad en memoria durante toda la vida del proceso.
// ═══════════════════════════════════════════════════════════════════════════════


@Repository  // ← SINGLETON: Spring instancia esta clase una sola vez en todo el contexto
public class PersonaDonanteRepositorioEnMemoria implements PersonaDonanteRepositorio {

    // LÍNEA CLAVE (SINGLETON): instancia única controlada por Spring.
    // Si necesitaras Singleton puro sin Spring, usarías un campo static + getInstance().
    // Con Spring, @Repository + constructor sin argumentos lo garantiza igual.

    // el Map es el estado compartido de toda la aplicación en Entrega 1
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
