package com.donatrack.donaciones.repositorio;

import com.donatrack.donaciones.dominio.PersonaDonante;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato del repositorio de personas donantes.
 *
 * Decisión de diseño: se define como interfaz para desacoplar
 * la lógica de negocio del mecanismo de persistencia.
 * En Entrega 1 la implementación es en memoria; en Entrega 4
 * se reemplazará por una implementación JPA sin tocar el dominio ni el servicio.
 */
public interface PersonaDonanteRepositorio {

    PersonaDonante guardar(PersonaDonante personaDonante);

    Optional<PersonaDonante> buscarPorId(UUID id);

    Optional<PersonaDonante> buscarPorEmail(String email);

    List<PersonaDonante> buscarTodos();

    List<PersonaDonante> buscarActivos();

    void eliminar(UUID id);

    boolean existePorEmail(String email);
}
