package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.EntidadesBeneficiariasServiceInterface;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Campania;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntidadesBeneficiariasService implements EntidadesBeneficiariasServiceInterface {

    private final EntidadesBeneficiariasRepositoryInterface entidadesBeneficiariasRepository;


    public void guardar(EntidadBeneficiaria entidad) {
        entidadesBeneficiariasRepository.guardar(entidad);
    }

    public List<EntidadBeneficiaria> obtenerTodas() {
        return entidadesBeneficiariasRepository.buscarTodas();
    }

    public EntidadBeneficiaria obtenerPorId(UUID id) {
        return entidadesBeneficiariasRepository.obtenerPorId(id);
    }

    public void actualizar(UUID id, EntidadBeneficiaria entidadNueva) {

        EntidadBeneficiaria entidadExistente = entidadesBeneficiariasRepository.obtenerPorId(id);

        entidadExistente.setRazonSocial(entidadNueva.getRazonSocial());
        entidadExistente.setDireccion(entidadNueva.getDireccion());
        // entidadExistente.setMedioDeContacto(entidadNueva.getMedioDeContacto()); //MAL
        // TODO: Agregar bien los setters de MedioDeContacto y de Representante

        entidadesBeneficiariasRepository.guardar(entidadExistente);
    }

    public void agregarNecesidadACampania(UUID entidadId, UUID campaniaId, Necesidad nuevaNecesidad) {

        EntidadBeneficiaria entidad = entidadesBeneficiariasRepository.obtenerPorId(entidadId);

        Campania campaniaTarget = entidad.getCampanias().stream()
            .filter(campania -> campania.getIdCampania().equals(campaniaId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("La campaña no existe o no pertenece a esta entidad"));

        campaniaTarget.getNecesidades().add(nuevaNecesidad);

        entidadesBeneficiariasRepository.guardar(entidad);
    }

    public void actualizarPeriodos() {
        LocalDate hoy = LocalDate.now();
        List<EntidadBeneficiaria> entidades = entidadesBeneficiariasRepository.buscarTodas();
        /**
         * Recorre todas las entidades y reinicia el período de las NecesidadRecurrente
         * que hayan vencido respecto a la fecha de hoy.
         * Diseñar para ejecutarse como tarea programada diaria.
         */
        entidades.forEach(entidad ->
            entidad.getCampanias().stream()
                .flatMap(campania -> campania.getNecesidades().stream())
                .filter(NecesidadRecurrente.class::isInstance)
                .map(n -> (NecesidadRecurrente) n)
                .forEach(nr -> nr.obtenerOGenerarPeriodoActual(hoy))
        );
    }
}