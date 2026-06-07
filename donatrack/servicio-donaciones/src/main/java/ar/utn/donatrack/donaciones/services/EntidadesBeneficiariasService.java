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

    /**
     * Registra una única necesidad en una entidad beneficiaria.
     * Usado tanto por los tests como por el controller de alta individual.
     */
    public void registrarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad) {
        entidad.registrarNecesidad(necesidad);
        entidadesBeneficiariasRepository.guardar(entidad);
    }

    /**
     * Registra múltiples necesidades agrupadas en una campaña.
     * Si fechaInicio es null, usa la fecha de hoy.
     */
    public void registrarNecesidades(EntidadBeneficiaria entidad, Campania campania) {
        campania.getNecesidades().forEach(entidad::registrarNecesidad);
        entidadesBeneficiariasRepository.guardar(entidad);
    }

    /**
     * Recorre todas las entidades y reinicia el período de las NecesidadRecurrente
     * que hayan vencido respecto a la fecha de hoy.
     * Diseñado para ejecutarse como tarea programada diaria.
     */
    public void actualizarPeriodos() {
        LocalDate hoy = LocalDate.now();
        List<EntidadBeneficiaria> entidades = entidadesBeneficiariasRepository.buscarTodas();

        entidades.forEach(entidad ->
            entidad.getNecesidades().stream()
                .filter(NecesidadRecurrente.class::isInstance)
                .map(n -> (NecesidadRecurrente) n)
                .forEach(nr -> nr.obtenerOGenerarPeriodoActual(hoy))
        );
    }
}