package ar.utn.donatrack.incentivos.services;

import ar.utn.donatrack.incentivos.interfaces.services.DonacionesQueryPort;
import ar.utn.donatrack.incentivos.interfaces.services.DonacionesQueryPort.DonacionResumen;
import ar.utn.donatrack.incentivos.models.EvolucionPeriodo;
import ar.utn.donatrack.incentivos.models.MetricasDonante;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AnaliticaDonanteService {

    private final DonacionesQueryPort donacionesQueryPort;

    public MetricasDonante calcularMetricas(UUID donanteId) {
        List<DonacionResumen> donaciones = donacionesQueryPort.obtenerDonacionesDe(donanteId);

        int totalHistorico = donaciones.size();

        long organizacionesAyudadas = donaciones.stream()
                .map(DonacionResumen::entidadBeneficiariaId) //ver biern estas entidades
                .distinct() //elimina los elementos duplicados
                .count(); //es como un length

        List<EvolucionPeriodo> evolucion = donaciones.stream()
                .collect(Collectors.groupingBy(
                        d -> Map.entry(d.fecha().getYear(), d.fecha().getMonthValue()),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> EvolucionPeriodo.builder()
                        .anio(entry.getKey().getKey())
                        .mes(entry.getKey().getValue())
                        .cantidadDonaciones(entry.getValue().intValue())
                        .build())
                .sorted(Comparator.comparing(EvolucionPeriodo::getAnio) //Ordena cronologicamente primero compara anio y despues mes
                        .thenComparing(EvolucionPeriodo::getMes))
                .toList();

        return MetricasDonante.builder()
                .donanteId(donanteId)
                .totalDonacionesHistoricas(totalHistorico)
                .organizacionesAyudadas((int) organizacionesAyudadas)
                .evolucionPorPeriodo(evolucion)
                .build();
    }
}