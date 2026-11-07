package ar.utn.donatrack.incentivos.models;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MetricasDonante {

    public int totalDonacionesHistoricas(Donante donante) {
        return (int) donante.getDonaciones().stream()
                .filter(donacion -> !donacion.isExitosa())
                .count();
    }

    public int organizacionesAyudadas(Donante donante) {
        return (int) donante.getDonaciones().stream()
                .filter(DonacionRegistrada::isExitosa)
                .map(DonacionRegistrada::getEntidadBeneficiaria)
                .filter(entidad -> entidad != null && !entidad.isBlank())
                .distinct()
                .count();
    }

    public List<EvolucionPeriodo> evolucionPorPeriodo(Donante donante) {
        return donante.getDonaciones().stream()
                .filter(donacion -> !donacion.isExitosa())
                .collect(Collectors.groupingBy(
                        donacion -> YearMonth.from(donacion.getFecha()),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> EvolucionPeriodo.builder()
                        .anio(entry.getKey().getYear())
                        .mes(entry.getKey().getMonthValue())
                        .cantidadDonaciones(entry.getValue().intValue())
                        .build())
                .toList();
    }

    public int donacionesMesActual(Donante donante) {
        return donacionesEnPeriodo(donante, YearMonth.now());
    }

    public int donacionesExitosas(Donante donante) {
        return (int) donante.getDonaciones().stream()
                .filter(DonacionRegistrada::isExitosa)
                .count();
    }

    public int recordBienesUnicaDonacion(Donante donante) {
        return donante.getDonaciones().stream()
                .filter(donacion -> !donacion.isExitosa())
                .mapToInt(DonacionRegistrada::getCantidadBienes)
                .max()
                .orElse(0);
    }

    public int categoriasDistintasDonadas(Donante donante) {
        Set<String> categorias = new HashSet<>();
        donante.getDonaciones().stream()
                .filter(donacion -> !donacion.isExitosa())
                .forEach(donacion -> categorias.addAll(donacion.getCategorias()));
        return categorias.size();
    }

    private int donacionesEnPeriodo(Donante donante, YearMonth periodo) {
        return (int) donante.getDonaciones().stream()
                .filter(donacion -> !donacion.isExitosa())
                .filter(donacion -> YearMonth.from(donacion.getFecha()).equals(periodo))
                .count();
    }
}
