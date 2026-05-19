package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.model.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.model.entidad.Necesidad;
import ar.utn.donatrack.donaciones.model.entidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.repositories.EntidadesBeneficiariasRepository;

import java.util.List;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntidadesBeneficiariasService {

    private final EntidadesBeneficiariasRepository entidadesBeneficiariasRepository;

    private static EntidadesBeneficiariasService instance = null;

    private EntidadesBeneficiariasService(EntidadesBeneficiariasRepository entidadesBeneficiariasRepository) {
        this.entidadesBeneficiariasRepository = entidadesBeneficiariasRepository;
    }

    public static EntidadesBeneficiariasService instance() {
        if(instance == null) {
            instance = new EntidadesBeneficiariasService(EntidadesBeneficiariasRepository.instance());
        }
        return instance;
    }

    public void registrarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad) {
        entidadesBeneficiariasRepository.cargarNecesidad(entidad, necesidad);
    }

    //relacionado con los periodos de las necesidades recurrentes de las entidades beneficiarias
    
    public void reiniciarPeriodoNecesidadRecurrente(EntidadBeneficiaria entidad, Necesidad necesidad) {
        if (necesidad instanceof NecesidadRecurrente) {
            ((NecesidadRecurrente) necesidad).reiniciarPeriodo();
        }
    }

    public void actualizarPeriodos() {
    List<EntidadBeneficiaria> entidades = entidadesBeneficiariasRepository.buscarTodas();
    LocalDate hoy = LocalDate.now();
    entidades.forEach(entidad -> entidad.getNecesidades().stream()
                .filter(n -> n instanceof NecesidadRecurrente)
                .map(n -> (NecesidadRecurrente) n)
                .filter(necesidadRecurrente -> necesidadRecurrente.requiereReinicio(hoy)) // Polimorfismo puro
                .forEach(NecesidadRecurrente::reiniciarPeriodo)
        );
    }
}