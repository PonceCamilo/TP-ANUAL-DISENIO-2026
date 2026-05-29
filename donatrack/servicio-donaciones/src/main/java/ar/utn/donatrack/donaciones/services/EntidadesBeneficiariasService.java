package ar.utn.donatrack.donaciones.services;

import ar.utn.donatrack.donaciones.interfaces.repositories.EntidadesBeneficiariasRepositoryInterface;
import ar.utn.donatrack.donaciones.interfaces.services.EntidadesBeneficiariasServiceInterface;
import ar.utn.donatrack.donaciones.models.entidad.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.Necesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.CargaNecesidad;
import ar.utn.donatrack.donaciones.models.entidad.necesidad.NecesidadRecurrente;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntidadesBeneficiariasService implements EntidadesBeneficiariasServiceInterface {

    private final EntidadesBeneficiariasRepositoryInterface entidadesBeneficiariasRepository;

    //public void registrarNecesidad(EntidadBeneficiaria entidad, Necesidad necesidad) {
    //    entidadesBeneficiariasRepository.cargarNecesidad(entidad, necesidad);
    //}

    // ahora queremos poder cargar varias necesidades a la vez con la clase cargaNecesidad 

    public void registrarNecesidades(EntidadBeneficiaria entidad, CargaNecesidad carga) {
        if (carga.getFechaIngreso() == null) {
            carga.setFechaIngreso(LocalDateTime.now());
        }
        LocalDate hoy = carga.getFechaIngreso().toLocalDate();
        for (Necesidad necesidad : carga.getNecesidades()) {
            // Si la necesidad es recurrente, inicializamos su período automáticamente hoy
            if (necesidad instanceof NecesidadRecurrente necesidadRecurrente) {
                necesidadRecurrente.obtenerOGenerarPeriodoActual(hoy);
            }
            entidad.registrarNecesidad(necesidad);
        }
    }

    //public void reiniciarPeriodoNecesidadRecurrente(NecesidadRecurrente necesidad) {
    //    necesidad.reiniciarPeriodo();
    //}

    public void actualizarPeriodos() {
    List<EntidadBeneficiaria> entidades = entidadesBeneficiariasRepository.buscarTodas();
    LocalDate hoy = LocalDate.now();
    entidades.forEach(entidad -> entidad.getNecesidades().stream()
                .filter(n -> n instanceof NecesidadRecurrente)
                .map(n -> (NecesidadRecurrente) n)
                .filter(necesidadRecurrente -> necesidadRecurrente.periodoVencido(hoy))
                .forEach(necesidadRecurrente -> necesidadRecurrente.obtenerOGenerarPeriodoActual(hoy))  // crea el periodo
        );
    }
}