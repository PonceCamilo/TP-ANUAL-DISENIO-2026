package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.services.EntidadesBeneficiariasServiceInterface;
import ar.utn.donatrack.donaciones.model.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.model.entidad.Necesidad;
import ar.utn.donatrack.donaciones.model.entidad.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.repositories.EntidadesBeneficiariasRepository;

import java.util.List;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntidadesBeneficiariasService implements EntidadesBeneficiariasServiceInterface {

    private final EntidadesBeneficiariasRepository entidadesBeneficiariasRepository;

    public void registrarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad) {
        entidadesBeneficiariasRepository.cargarNecesidad(entidad, necesidad);
    }

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