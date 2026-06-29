package ar.utn.donatrack.incentivos.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricasDonante {
    @Builder.Default
    private List<EvolucionPeriodo> donaciones = new ArrayList<>();

    public void registrarDonacion(int cantidadBienes, List<String> categoriasDonadas) {
        donaciones.add(EvolucionPeriodo.builder()
                .fecha(LocalDate.now())
                .cantidadBienes(cantidadBienes)
                .categoriasDonadas(categoriasDonadas == null ? List.of() : List.copyOf(categoriasDonadas))
                .exitosa(false)
                .build());
    }

    public void registrarDonacionExitosa() {
        donaciones.add(EvolucionPeriodo.builder()
                .fecha(LocalDate.now())
                .cantidadBienes(0)
                .categoriasDonadas(List.of())
                .exitosa(true)
                .build());
    }

    public int totalDonacionesHistoricas() {
        return (int) donaciones.stream().filter(d -> !d.isExitosa()).count();
    }

    public int donacionesExitosas() {
        return (int) donaciones.stream().filter(EvolucionPeriodo::isExitosa).count();
    }

    public int organizacionesAyudadas() {
        return donacionesExitosas();
    }

    public int recordBienesUnicaDonacion() {
        return donaciones.stream()
                .mapToInt(EvolucionPeriodo::getCantidadBienes)
                .max()
                .orElse(0);
    }

    public int categoriasDistintasDonadas() {
        Set<String> categorias = new HashSet<>();
        donaciones.forEach(d -> categorias.addAll(d.getCategoriasDonadas()));
        return categorias.size();
    }

    public int donacionesMesActual() {
        YearMonth actual = YearMonth.now();
        return donacionesEnPeriodo(actual);
    }

    public int mesesConsecutivosDonando(LocalDate fechaReferencia) {
        YearMonth cursor = YearMonth.from(fechaReferencia);
        int meses = 0;

        while (donacionesEnPeriodo(cursor) > 0) {
            meses++;
            cursor = cursor.minusMonths(1);
        }

        return meses;
    }

    public boolean pasoUnMesCompletoSinDonaciones(LocalDate fechaReferencia) {
        if (donaciones.isEmpty()) {
            return false;
        }
        YearMonth mesAnterior = YearMonth.from(fechaReferencia).minusMonths(1);
        YearMonth ultimoMesConDonacion = donaciones.stream()
                .filter(d -> !d.isExitosa())
                .map(d -> YearMonth.from(d.getFecha()))
                .max(Comparator.naturalOrder())
                .orElse(mesAnterior);

        return ultimoMesConDonacion.isBefore(mesAnterior);
    }

    private int donacionesEnPeriodo(YearMonth periodo) {
        return (int) donaciones.stream()
                .filter(d -> !d.isExitosa())
                .filter(d -> YearMonth.from(d.getFecha()).equals(periodo))
                .count();
    }
}
